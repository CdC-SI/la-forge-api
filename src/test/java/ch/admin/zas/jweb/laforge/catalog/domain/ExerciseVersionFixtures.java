package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Fabrique de données de test réutilisée par les tests de persistance de plusieurs domaines
 * (catalog, practice, review, collective, discovery, authoring) qui dépendent tous d'une
 * {@link ExerciseVersion} richement peuplée (tous les sous-documents {@code jsonb}), afin de
 * vérifier le round-trip de sérialisation Jackson à travers les {@code AttributeConverter}.
 */
public final class ExerciseVersionFixtures {

    private ExerciseVersionFixtures() {
    }

    public static List<CodeFile> sampleFiles() {
        return List.of(new CodeFile("src/Main.java", "java", "class Main {}", CodeFileKind.SOURCE));
    }

    public static List<TechnologyRequirement> sampleTechnologies() {
        return List.of(new TechnologyRequirement("Java", "25", FeatureStatus.STABLE, "Notes sur la version."));
    }

    public static List<Hint> sampleHints() {
        return List.of(new Hint(1, "Premier indice"), new Hint(2, "Second indice"));
    }

    public static ResponseSpec sampleResponseSpec() {
        return new ResponseSpec(ResponseKind.SINGLE_CHOICE, List.of(new Choice("a", "Option A"), new Choice("b", "Option B")));
    }

    public static Correction sampleCorrection() {
        return new Correction(
                "Explication du corrigé",
                List.of("réponse acceptée"),
                List.of("a"),
                List.of(ReviewVerdict.APPROVE),
                List.of(new RubricCriterion("c1", "Critère 1", RubricImportance.MAJOR, "Explication du critère")),
                List.of(new Source("Doc officielle", "https://example.com/doc", OffsetDateTime.parse("2024-01-01T00:00:00Z"))));
    }

    public static ExerciseVersion sampleExerciseVersion(Exercise exercise, int versionNumber, Set<Topic> topics) {
        return new ExerciseVersion(
                exercise,
                versionNumber,
                "Titre de l'exercice",
                ExerciseType.CODE_REVIEW,
                Difficulty.INTERMEDIATE,
                20,
                "Énoncé en *markdown*",
                List.of("Objectif 1", "Objectif 2"),
                sampleFiles(),
                sampleTechnologies(),
                sampleResponseSpec(),
                sampleHints(),
                sampleCorrection(),
                topics);
    }
}
