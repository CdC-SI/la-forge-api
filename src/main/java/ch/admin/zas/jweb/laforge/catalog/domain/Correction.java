package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.domain.Source;
import java.util.List;

/**
 * Corrigé d'un exercice, jamais exposé à l'apprenant avant soumission. Selon {@link ResponseKind}
 * de l'exercice : {@code correctChoiceIds} pour un choix, {@code expectedVerdicts} pour une revue,
 * {@code acceptedAnswers}/{@code criteria} en complément pour les réponses libres et guidées.
 */
public record Correction(
        String explanationMarkdown,
        List<String> acceptedAnswers,
        List<String> correctChoiceIds,
        List<ReviewVerdict> expectedVerdicts,
        List<RubricCriterion> criteria,
        List<Source> sources) {

    public Correction {
        acceptedAnswers = acceptedAnswers == null ? List.of() : List.copyOf(acceptedAnswers);
        correctChoiceIds = correctChoiceIds == null ? List.of() : List.copyOf(correctChoiceIds);
        expectedVerdicts = expectedVerdicts == null ? List.of() : List.copyOf(expectedVerdicts);
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
