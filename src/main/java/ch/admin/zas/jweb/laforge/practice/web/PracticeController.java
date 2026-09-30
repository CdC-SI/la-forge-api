package ch.admin.zas.jweb.laforge.practice.web;

import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.common.idempotency.Idempotent;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.dto.AttemptDto;
import ch.admin.zas.jweb.laforge.practice.dto.CreateAttemptInput;
import ch.admin.zas.jweb.laforge.practice.dto.DebriefDto;
import ch.admin.zas.jweb.laforge.practice.dto.HintRequestInput;
import ch.admin.zas.jweb.laforge.practice.dto.SelfAssessmentInput;
import ch.admin.zas.jweb.laforge.practice.dto.SubmitAnswerInput;
import ch.admin.zas.jweb.laforge.practice.service.PracticeService;
import ch.admin.zas.jweb.laforge.review.dto.ReviewItemDto;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cycle de vie des tentatives de pratique : création, soumission, indices, débrief, autoévaluation (tag {@code Practice}). */
@RestController
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public AttemptDto createAttempt(@CurrentAccount CurrentAccountDto account, @Valid @RequestBody CreateAttemptInput input) {
        return practiceService.createAttempt(account, input);
    }

    @GetMapping("/me/attempts")
    public Page<AttemptDto> listMyAttempts(
            @CurrentAccount CurrentAccountDto account,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) AttemptStatus status) {
        return practiceService.listMyAttempts(account, PageQuery.of(limit, cursor), status);
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDto getAttempt(@CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId) {
        return practiceService.getAttempt(account, attemptId);
    }

    @PutMapping("/attempts/{attemptId}/submission")
    public AttemptDto submitAttempt(
            @CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId, @Valid @RequestBody SubmitAnswerInput input) {
        return practiceService.submitAttempt(account, attemptId, input.answer());
    }

    @PostMapping("/attempts/{attemptId}/abandon")
    public AttemptDto abandonAttempt(@CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId) {
        return practiceService.abandonAttempt(account, attemptId);
    }

    @PostMapping("/attempts/{attemptId}/hints")
    public Hint revealHint(
            @CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId, @Valid @RequestBody HintRequestInput input) {
        return practiceService.revealHint(account, attemptId, input.level());
    }

    @GetMapping("/attempts/{attemptId}/debrief")
    public DebriefDto getDebrief(@CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId) {
        return practiceService.getDebrief(account, attemptId);
    }

    @PutMapping("/attempts/{attemptId}/self-assessment")
    public ReviewItemDto setSelfAssessment(
            @CurrentAccount CurrentAccountDto account, @PathVariable UUID attemptId, @Valid @RequestBody SelfAssessmentInput input) {
        return practiceService.setSelfAssessment(account, attemptId, input.mastery(), input.note());
    }
}
