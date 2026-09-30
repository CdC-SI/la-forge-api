package ch.admin.zas.jweb.laforge.practice.domain;

/**
 * Niveau de maîtrise perçu par l'apprenant après consultation du corrigé. Détermine le délai
 * avant la prochaine échéance de révision (voir l'algorithme documenté sur {@code ReviewItem}).
 */
public enum SelfAssessmentMastery {
    AGAIN,
    HARD,
    GOOD,
    EASY
}
