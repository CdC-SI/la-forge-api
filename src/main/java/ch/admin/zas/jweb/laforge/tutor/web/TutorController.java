package ch.admin.zas.jweb.laforge.tutor.web;

import ch.admin.zas.jweb.laforge.common.idempotency.Idempotent;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.ratelimit.RateLimited;
import ch.admin.zas.jweb.laforge.common.ratelimit.RateLimitFamily;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import ch.admin.zas.jweb.laforge.tutor.dto.TutorExchangeDto;
import ch.admin.zas.jweb.laforge.tutor.dto.TutorQuestionInput;
import ch.admin.zas.jweb.laforge.tutor.service.TutorService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Échanges avec le tuteur assisté par IA, désactivé en v1 (tag {@code Tutor}). */
@RestController
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/attempts/{attemptId}/tutor-exchanges")
    public Page<TutorExchangeDto> listTutorExchanges(
            @CurrentAccount Account account,
            @PathVariable UUID attemptId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return tutorService.listTutorExchanges(account, attemptId, PageQuery.of(limit, cursor));
    }

    @PostMapping("/attempts/{attemptId}/tutor-exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    @RateLimited(RateLimitFamily.TUTOR)
    public TutorExchangeDto askTutor(
            @CurrentAccount Account account, @PathVariable UUID attemptId, @Valid @RequestBody TutorQuestionInput input) {
        return tutorService.askTutor(account, attemptId, input.question());
    }
}
