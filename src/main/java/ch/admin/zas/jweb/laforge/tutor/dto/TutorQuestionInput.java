package ch.admin.zas.jweb.laforge.tutor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /attempts/{attemptId}/tutor-exchanges}. */
public record TutorQuestionInput(@NotBlank @Size(max = 4000) String question) {
}
