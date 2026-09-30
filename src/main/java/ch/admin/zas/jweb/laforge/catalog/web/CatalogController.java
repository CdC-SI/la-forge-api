package ch.admin.zas.jweb.laforge.catalog.web;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicDto;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lecture publique (authentifiée) du catalogue : thèmes et exercices publiés (tag {@code Catalog}). */
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

    @GetMapping("/exercises")
    public Page<ExerciseSummaryDto> listExercises(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) ExerciseType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false) String q) {
        return catalogService.listExercises(PageQuery.of(limit, cursor), type, difficulty, topicId, technology, q);
    }

    @GetMapping("/exercises/{exerciseId}")
    public ExerciseDto getLatestExercise(@PathVariable UUID exerciseId) {
        return catalogService.getLatestExercise(exerciseId);
    }

    @GetMapping("/exercises/{exerciseId}/versions/{version}")
    public ExerciseDto getExerciseVersion(@PathVariable UUID exerciseId, @PathVariable int version) {
        return catalogService.getExerciseVersion(exerciseId, version);
    }
}
