package ch.admin.zas.jweb.laforge.practice.dto;

import ch.admin.zas.jweb.laforge.practice.domain.Answer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corps de {@code PUT /attempts/{attemptId}/submission}. */
public record SubmitAnswerInput(@Valid @NotNull Answer answer) {
}
