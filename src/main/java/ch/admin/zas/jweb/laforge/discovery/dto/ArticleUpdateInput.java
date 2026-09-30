package ch.admin.zas.jweb.laforge.discovery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Corps de {@code PUT /authoring/articles/{articleId}} : verrouillage optimiste explicite. */
public record ArticleUpdateInput(@Min(1) int expectedRevision, @Valid @NotNull ArticleInput content) {
}
