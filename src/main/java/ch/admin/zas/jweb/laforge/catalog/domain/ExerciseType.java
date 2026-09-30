package ch.admin.zas.jweb.laforge.catalog.domain;

/** Type d'exercice, déterminant la forme d'énoncé et de correction attendue. */
public enum ExerciseType {
    CODE_REVIEW,
    PREDICTION,
    DIAGNOSIS,
    REFACTORING,
    TECH_DISCOVERY,
    TECHNICAL_CHOICE,
    QUIZ
}
