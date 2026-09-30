package ch.admin.zas.jweb.laforge.collective.web;

import ch.admin.zas.jweb.laforge.collective.dto.ChallengeCreatedDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeInput;
import ch.admin.zas.jweb.laforge.collective.dto.CommentInput;
import ch.admin.zas.jweb.laforge.collective.dto.DiscussionCommentDto;
import ch.admin.zas.jweb.laforge.collective.dto.JoinChallengeInput;
import ch.admin.zas.jweb.laforge.collective.dto.SharedResponseDto;
import ch.admin.zas.jweb.laforge.collective.service.CollectiveService;
import ch.admin.zas.jweb.laforge.common.idempotency.Idempotent;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
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

/** Défis collectifs : création, adhésion, comparaison des réponses et discussion (tag {@code Collective}). */
@RestController
public class CollectiveController {

    private final CollectiveService collectiveService;

    public CollectiveController(CollectiveService collectiveService) {
        this.collectiveService = collectiveService;
    }

    @PostMapping("/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ChallengeCreatedDto createChallenge(@CurrentAccount Account account, @Valid @RequestBody ChallengeInput input) {
        return collectiveService.createChallenge(account, input);
    }

    @GetMapping("/me/challenges")
    public Page<ChallengeDto> listMyChallenges(
            @CurrentAccount Account account,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return collectiveService.listMyChallenges(account, PageQuery.of(limit, cursor));
    }

    @PostMapping("/challenges/join")
    public ChallengeDto joinChallenge(@CurrentAccount Account account, @Valid @RequestBody JoinChallengeInput input) {
        return collectiveService.joinChallenge(account, input.joinCode());
    }

    @GetMapping("/challenges/{challengeId}")
    public ChallengeDto getChallenge(@CurrentAccount Account account, @PathVariable UUID challengeId) {
        return collectiveService.getChallenge(account, challengeId);
    }

    @PostMapping("/challenges/{challengeId}/close")
    public ChallengeDto closeChallenge(@CurrentAccount Account account, @PathVariable UUID challengeId) {
        return collectiveService.closeChallenge(account, challengeId);
    }

    @GetMapping("/challenges/{challengeId}/responses")
    public Page<SharedResponseDto> listSharedResponses(
            @CurrentAccount Account account,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return collectiveService.listSharedResponses(account, challengeId, PageQuery.of(limit, cursor));
    }

    @GetMapping("/challenges/{challengeId}/comments")
    public Page<DiscussionCommentDto> listChallengeComments(
            @CurrentAccount Account account,
            @PathVariable UUID challengeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return collectiveService.listChallengeComments(account, challengeId, PageQuery.of(limit, cursor));
    }

    @PostMapping("/challenges/{challengeId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public DiscussionCommentDto createChallengeComment(
            @CurrentAccount Account account, @PathVariable UUID challengeId, @Valid @RequestBody CommentInput input) {
        return collectiveService.createChallengeComment(account, challengeId, input.body());
    }
}
