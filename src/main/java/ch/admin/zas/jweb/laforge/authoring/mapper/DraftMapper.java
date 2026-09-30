package ch.admin.zas.jweb.laforge.authoring.mapper;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftDto;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;

/** Projection du contenu éditorial, sans accès aux données. */
public final class DraftMapper {

    private DraftMapper() {
    }

    public static DraftDto toDto(Draft draft) {
        return new DraftDto(
                draft.getId(), draft.getExercise().getId(), draft.getAuthor().getId(),
                draft.getState(), draft.getRevision(), draft.getBaseVersion(),
                toContent(draft), draft.getPublishedVersion(), draft.getUpdatedAt());
    }

    public static ExerciseContentInput toContent(Draft draft) {
        return new ExerciseContentInput(
                draft.getTitle(), draft.getType(), draft.getDifficulty(),
                draft.getTopics().stream().map(BaseEntity::getId).toList(),
                draft.getEstimatedMinutes(), draft.getTechnologies(), draft.getPromptMarkdown(),
                draft.getLearningObjectives(), draft.getFiles(), draft.getResponseSpec(),
                draft.getHints(), draft.getCorrection());
    }
}
