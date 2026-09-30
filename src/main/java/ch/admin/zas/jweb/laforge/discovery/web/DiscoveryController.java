package ch.admin.zas.jweb.laforge.discovery.web;

import ch.admin.zas.jweb.laforge.common.idempotency.Idempotent;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleDto;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleInput;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleSummaryDto;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleUpdateInput;
import ch.admin.zas.jweb.laforge.discovery.service.DiscoveryService;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Fiches de veille technique : lecture publique et publication éditoriale (tags {@code Discoveries}/{@code Authoring}). */
@RestController
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/articles")
    public Page<ArticleSummaryDto> listArticles(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false) String q) {
        return discoveryService.listArticles(PageQuery.of(limit, cursor), topicId, technology, q);
    }

    @GetMapping("/articles/{articleId}")
    public ArticleDto getArticle(@CurrentAccount CurrentAccountDto account, @PathVariable UUID articleId) {
        return discoveryService.getArticle(account, articleId);
    }

    @PostMapping("/authoring/articles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    @Idempotent
    public ArticleDto publishArticle(@CurrentAccount CurrentAccountDto account, @Valid @RequestBody ArticleInput input) {
        return discoveryService.publishArticle(account, input);
    }

    @PutMapping("/authoring/articles/{articleId}")
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ArticleDto updateArticle(@CurrentAccount CurrentAccountDto account, @PathVariable UUID articleId,
            @Valid @RequestBody ArticleUpdateInput input) {
        return discoveryService.updateArticle(account, articleId, input.expectedRevision(), input.content());
    }
}
