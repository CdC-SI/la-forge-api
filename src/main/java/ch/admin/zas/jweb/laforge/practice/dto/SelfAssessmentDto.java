package ch.admin.zas.jweb.laforge.practice.dto;

import ch.admin.zas.jweb.laforge.practice.domain.SelfAssessmentMastery;

/** Autoévaluation courante d'une tentative, telle que reflétée dans le débrief. */
public record SelfAssessmentDto(SelfAssessmentMastery mastery, String note) {
}
