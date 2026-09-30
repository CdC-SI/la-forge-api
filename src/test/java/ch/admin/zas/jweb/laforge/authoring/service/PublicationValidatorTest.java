package ch.admin.zas.jweb.laforge.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.catalog.domain.Choice;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.ReviewVerdict;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.error.Violation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class PublicationValidatorTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final PublicationValidator validator = new PublicationValidator(FACTORY.getValidator());

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void titleOnlyCanBeSavedButCannotBePublished() {
        var content = new ExerciseContentInput("Titre", null, null, null, null, null, null, null, null, null, null, null);
        assertThat(FACTORY.getValidator().validate(content)).isEmpty();
        assertThatThrownBy(() -> validator.validate(content)).isInstanceOfSatisfying(
                ValidationFailedException.class, exception -> assertThat(exception.violations())
                        .extracting(Violation::field).contains("content.type", "content.difficulty",
                                "content.estimatedMinutes", "content.promptMarkdown", "content.topicIds",
                                "content.learningObjectives", "content.responseSpec", "content.correction"));
    }

    @Test
    void completeQuizWithTechnologyNameOnlyCanBePublished() {
        assertThatCode(() -> validator.validate(content(
                new ResponseSpec(ResponseKind.SINGLE_CHOICE, choices()),
                correction(List.of("a"), List.of()), List.of(new Hint(1, "Indice")))))
                .doesNotThrowAnyException();
    }

    @Test
    void quizCannotUseFreeText() {
        assertInvalid(content(new ResponseSpec(ResponseKind.FREE_TEXT, List.of()),
                correction(List.of(), List.of()), List.of()), "content.responseSpec.kind");
    }

    @Test
    void choicesMustBeUniqueAndCorrectChoiceMustExist() {
        assertInvalid(content(new ResponseSpec(ResponseKind.SINGLE_CHOICE,
                List.of(new Choice("a", "A"), new Choice("a", "Encore A"))),
                correction(List.of("missing"), List.of()), List.of()),
                "content.responseSpec.choices", "content.correction.correctChoiceIds");
    }

    @Test
    void singleChoiceRequiresExactlyOneCorrectAnswer() {
        assertInvalid(content(new ResponseSpec(ResponseKind.SINGLE_CHOICE, choices()),
                correction(List.of("a", "b"), List.of()), List.of()), "content.correction.correctChoiceIds");
    }

    @Test
    void hintsMustBeContiguousAndReviewVerdictsCannotAppearInQuiz() {
        assertInvalid(content(new ResponseSpec(ResponseKind.SINGLE_CHOICE, choices()),
                correction(List.of("a"), List.of(ReviewVerdict.APPROVE)), List.of(new Hint(2, "Indice"))),
                "content.hints[0].level", "content.correction.expectedVerdicts");
    }

    @Test
    void correctionRequiresExplanationAndSources() {
        assertInvalid(content(new ResponseSpec(ResponseKind.SINGLE_CHOICE, choices()),
                new Correction(null, null, List.of("a"), null, null, null), List.of()),
                "content.correction.explanationMarkdown", "content.correction.sources");
    }

    private void assertInvalid(ExerciseContentInput content, String... fields) {
        assertThatThrownBy(() -> validator.validate(content)).isInstanceOfSatisfying(
                ValidationFailedException.class, exception ->
                        assertThat(exception.violations()).extracting(Violation::field).contains(fields));
    }

    private List<Choice> choices() {
        return List.of(new Choice("a", "A"), new Choice("b", "B"));
    }

    private Correction correction(List<String> correctIds, List<ReviewVerdict> verdicts) {
        return new Correction("Explication", List.of(), correctIds, verdicts, List.of(),
                List.of(new Source("Documentation", "https://example.com/doc", OffsetDateTime.now())));
    }

    private ExerciseContentInput content(ResponseSpec spec, Correction correction, List<Hint> hints) {
        return new ExerciseContentInput("Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(UUID.randomUUID()),
                5, List.of(new TechnologyRequirement("Java", null, null, null)), "Question", List.of("Objectif"),
                List.of(), spec, hints, correction);
    }
}
