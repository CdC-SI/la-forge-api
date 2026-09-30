package ch.admin.zas.jweb.laforge.practice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ch.admin.zas.jweb.laforge.practice.domain.SelfAssessmentMastery;

/** Corps de {@code PUT /attempts/{attemptId}/self-assessment}. */
public record SelfAssessmentInput(@NotNull SelfAssessmentMastery mastery, @Size(max = 2000) String note) {
}
