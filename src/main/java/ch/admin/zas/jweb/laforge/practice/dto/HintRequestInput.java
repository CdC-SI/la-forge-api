package ch.admin.zas.jweb.laforge.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Corps de {@code POST /attempts/{attemptId}/hints}. */
public record HintRequestInput(@Min(1) @Max(5) int level) {
}
