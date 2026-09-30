package ch.admin.zas.jweb.laforge.authoring.web;

import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftDto;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftInput;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftUpdateInput;
import ch.admin.zas.jweb.laforge.authoring.dto.ReviewInput;
import ch.admin.zas.jweb.laforge.authoring.service.AuthoringService;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.common.concurrency.ETagSupport;
import ch.admin.zas.jweb.laforge.common.idempotency.Idempotent;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Brouillons et machine à états éditoriale (tag {@code Authoring}, chemins {@code /authoring/drafts/**}). */
@RestController
public class DraftController {

    private final AuthoringService authoringService;

    public DraftController(AuthoringService authoringService) {
        this.authoringService = authoringService;
    }

    @GetMapping("/authoring/drafts")
    @PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'ADMIN')")
    public Page<DraftDto> listDrafts(
            @CurrentAccount Account account,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) DraftState state) {
        return authoringService.listDrafts(account, PageQuery.of(limit, cursor), state);
    }

    @PostMapping("/authoring/drafts")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    @Idempotent
    public ResponseEntity<DraftDto> createDraft(@CurrentAccount Account account, @Valid @RequestBody DraftInput input) {
        var draft = authoringService.createDraft(account, input.exerciseId(), input.content());
        return ETagSupport.withETag(HttpStatus.CREATED, draft.revision(), draft);
    }

    @GetMapping("/authoring/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'ADMIN')")
    public ResponseEntity<DraftDto> getDraft(@CurrentAccount Account account, @PathVariable UUID draftId) {
        var draft = authoringService.getDraft(account, draftId);
        return ETagSupport.withETag(HttpStatus.OK, draft.revision(), draft);
    }

    @PutMapping("/authoring/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<DraftDto> replaceDraft(
            @CurrentAccount Account account,
            @PathVariable UUID draftId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DraftUpdateInput input) {
        var draft = authoringService.replaceDraft(account, draftId, ETagSupport.requireRevision(ifMatch), input.content());
        return ETagSupport.withETag(HttpStatus.OK, draft.revision(), draft);
    }

    @PostMapping("/authoring/drafts/{draftId}/submit")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<DraftDto> submitDraftForReview(
            @CurrentAccount Account account,
            @PathVariable UUID draftId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        var draft = authoringService.submitDraftForReview(account, draftId, ETagSupport.requireRevision(ifMatch));
        return ETagSupport.withETag(HttpStatus.OK, draft.revision(), draft);
    }

    @PostMapping("/authoring/drafts/{draftId}/review")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<DraftDto> reviewDraft(
            @CurrentAccount Account account,
            @PathVariable UUID draftId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReviewInput input) {
        var draft = authoringService.reviewDraft(
                account, draftId, ETagSupport.requireRevision(ifMatch), input.decision(), input.comment());
        return ETagSupport.withETag(HttpStatus.OK, draft.revision(), draft);
    }

    @PostMapping("/authoring/drafts/{draftId}/publish")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ExerciseDto publishDraft(
            @CurrentAccount Account account,
            @PathVariable UUID draftId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return authoringService.publishDraft(account, draftId, ETagSupport.requireRevision(ifMatch));
    }
}
