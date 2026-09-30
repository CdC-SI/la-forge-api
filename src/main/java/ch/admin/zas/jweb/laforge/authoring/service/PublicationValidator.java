package ch.admin.zas.jweb.laforge.authoring.service;

import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.error.Violation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/** Vérifie le contenu complet uniquement au passage à la publication. */
@Component
public class PublicationValidator {

    private final Validator validator;

    public PublicationValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(ExerciseContentInput content) {
        var violations = new ArrayList<Violation>();
        validateBean(content, "content", violations);
        require(content.type() != null, "type", "Le type est requis.", violations);
        require(content.difficulty() != null, "difficulty", "La difficulté est requise.", violations);
        require(content.estimatedMinutes() != null, "estimatedMinutes", "La durée est requise.", violations);
        require(content.promptMarkdown() != null && !content.promptMarkdown().isBlank(),
                "promptMarkdown", "L'énoncé est requis.", violations);
        require(!content.topicIds().isEmpty(), "topicIds", "Au moins un thème est requis.", violations);
        require(new HashSet<>(content.topicIds()).size() == content.topicIds().size(),
                "topicIds", "Les thèmes doivent être uniques.", violations);
        require(!content.learningObjectives().isEmpty(), "learningObjectives",
                "Au moins un objectif est requis.", violations);
        require(content.responseSpec() != null, "responseSpec", "Le format de réponse est requis.", violations);
        require(content.correction() != null, "correction", "Le corrigé est requis.", violations);
        for (int i = 0; i < content.hints().size(); i++) {
            var hint = content.hints().get(i);
            require(hint.level() == i + 1, "hints[" + i + "].level",
                    "Les indices doivent être contigus à partir du niveau 1.", violations);
            requireText(hint.markdown(), 20000, "hints[" + i + "].markdown", violations);
        }
        if (content.responseSpec() != null && content.responseSpec().kind() != null) {
            var spec = content.responseSpec();
            var choiceResponse = spec.kind() == ResponseKind.SINGLE_CHOICE || spec.kind() == ResponseKind.MULTIPLE_CHOICE;
            require(content.type() != ExerciseType.QUIZ || choiceResponse, "responseSpec.kind",
                    "Un quiz exige une réponse à choix.", violations);
            if (choiceResponse) {
                require(spec.choices().size() >= 2 && spec.choices().size() <= 10,
                        "responseSpec.choices", "Entre 2 et 10 choix sont requis.", violations);
                var choiceIds = spec.choices().stream().map(choice -> choice.id()).toList();
                require(new HashSet<>(choiceIds).size() == choiceIds.size(), "responseSpec.choices",
                        "Les identifiants des choix doivent être uniques.", violations);
                if (content.correction() != null) {
                    var correctIds = content.correction().correctChoiceIds();
                    require(!correctIds.isEmpty() && choiceIds.containsAll(correctIds)
                                    && (spec.kind() != ResponseKind.SINGLE_CHOICE || correctIds.size() == 1),
                            "correction.correctChoiceIds", "Le corrigé doit référencer les choix proposés, un seul pour SINGLE_CHOICE.",
                            violations);
                }
            } else {
                require(spec.choices().isEmpty(), "responseSpec.choices",
                        "Ce format de réponse n'accepte pas de choix.", violations);
            }
            if (content.correction() != null) {
                var correction = content.correction();
                require(choiceResponse || correction.correctChoiceIds().isEmpty(), "correction.correctChoiceIds",
                        "Les bonnes réponses ne sont admises que pour un format à choix.", violations);
                require(spec.kind() != ResponseKind.REVIEW || !correction.expectedVerdicts().isEmpty(),
                        "correction.expectedVerdicts", "Au moins un verdict est requis pour une revue.", violations);
                require(spec.kind() == ResponseKind.REVIEW || correction.expectedVerdicts().isEmpty(),
                        "correction.expectedVerdicts", "Les verdicts ne sont admis que pour une revue.", violations);
            }
        }
        if (content.correction() != null) {
            validateCorrection(content.correction(), violations);
        }
        if (!violations.isEmpty()) {
            throw new ValidationFailedException("Le brouillon doit être complet et cohérent avant publication.", violations);
        }
    }

    private void validateCorrection(Correction correction, List<Violation> violations) {
        requireText(correction.explanationMarkdown(), 20000, "correction.explanationMarkdown", violations);
        require(correction.acceptedAnswers().size() <= 20, "correction.acceptedAnswers",
                "Au maximum 20 réponses sont admises.", violations);
        for (int i = 0; i < correction.acceptedAnswers().size(); i++) {
            requireText(correction.acceptedAnswers().get(i), 4000, "correction.acceptedAnswers[" + i + "]", violations);
        }
        require(correction.correctChoiceIds().size() <= 10
                        && new HashSet<>(correction.correctChoiceIds()).size() == correction.correctChoiceIds().size(),
                "correction.correctChoiceIds", "Au maximum 10 identifiants uniques sont admis.", violations);
        require(correction.expectedVerdicts().size() <= 3
                        && new HashSet<>(correction.expectedVerdicts()).size() == correction.expectedVerdicts().size(),
                "correction.expectedVerdicts", "Les verdicts doivent être uniques.", violations);
        require(correction.criteria().size() <= 20, "correction.criteria", "Au maximum 20 critères sont admis.", violations);
        for (int i = 0; i < correction.criteria().size(); i++) {
            var criterion = correction.criteria().get(i);
            var path = "correction.criteria[" + i + "]";
            requireText(criterion.id(), 50, path + ".id", violations);
            requireText(criterion.label(), 200, path + ".label", violations);
            require(criterion.importance() != null, path + ".importance", "L'importance est requise.", violations);
            requireText(criterion.explanationMarkdown(), 20000, path + ".explanationMarkdown", violations);
        }
        require(!correction.sources().isEmpty() && correction.sources().size() <= 20, "correction.sources",
                "Entre 1 et 20 sources sont requises.", violations);
        for (int i = 0; i < correction.sources().size(); i++) {
            validateBean(correction.sources().get(i), "content.correction.sources[" + i + "]", violations);
        }
    }

    private void validateBean(Object value, String path, List<Violation> violations) {
        validator.validate(value).forEach(violation ->
                violations.add(new Violation(path + "." + violation.getPropertyPath(), violation.getMessage())));
    }

    private static void requireText(String value, int maximum, String path, List<Violation> violations) {
        require(value != null && !value.isBlank() && value.length() <= maximum, path,
                "Une valeur non vide d'au maximum " + maximum + " caractères est requise.", violations);
    }

    private static void require(boolean condition, String path, String message, List<Violation> violations) {
        if (!condition) {
            violations.add(new Violation("content." + path, message));
        }
    }
}
