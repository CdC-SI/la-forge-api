package ch.admin.zas.jweb.laforge.practice.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import java.util.UUID;

/** Corrigé et résultat, disponible dès la soumission (schéma {@code Debrief}). */
public record DebriefDto(
        UUID attemptId,
        int exerciseVersion,
        Correction correction,
        ObjectiveResult objectiveResult,
        int usedHintCount,
        SelfAssessmentDto selfAssessment) {
}
