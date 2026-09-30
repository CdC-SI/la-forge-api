package ch.admin.zas.jweb.laforge.catalog.web;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicInput;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Catalogue : lecture (authentifiée) des thèmes et exercices publiés, ajout de thèmes (tag {@code Catalog}). */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/topics")
    public Page<TopicDto> listTopics(
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return catalogService.listTopics(PageQuery.of(limit, cursor));
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public TopicDto createTopic(@Valid @RequestBody TopicInput input) {
        return catalogService.createTopic(input);
    }

    @GetMapping("/exercises")
    public Page<ExerciseSummaryDto> listExercises(
            @CurrentAccount CurrentAccountDto account,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) ExerciseType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false) String q) {
        return catalogService.listExercises(account, PageQuery.of(limit, cursor), type, difficulty, topicId, technology, q);
    }

    @GetMapping("/exercises/{exerciseId}")
    public ExerciseDto getLatestExercise(@CurrentAccount CurrentAccountDto account, @PathVariable UUID exerciseId) {
        return catalogService.getLatestExercise(account, exerciseId);
    }

    @GetMapping("/exercises/{exerciseId}/versions/{version}")
    public ExerciseDto getExerciseVersion(
            @CurrentAccount CurrentAccountDto account, @PathVariable UUID exerciseId, @PathVariable int version) {
        return catalogService.getExerciseVersion(account, exerciseId, version);
    }
}
