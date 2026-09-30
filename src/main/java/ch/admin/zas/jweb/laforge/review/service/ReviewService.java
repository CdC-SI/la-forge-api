package ch.admin.zas.jweb.laforge.review.service;

import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewState;
import ch.admin.zas.jweb.laforge.review.dto.ReviewItemDto;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture des fiches de révision espacée de l'apprenant courant. L'état ({@link ReviewState}) est
 * dérivé à la volée de {@code completedAt}/{@code dueAt} par rapport à l'heure serveur, jamais
 * stocké : le filtre par défaut ({@code DUE}) est donc traduit en prédicat sur ces deux colonnes.
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewItemRepository reviewItemRepository;
    private final Clock clock;

    public ReviewService(ReviewItemRepository reviewItemRepository, Clock clock) {
        this.reviewItemRepository = reviewItemRepository;
        this.clock = clock;
    }

    /** Fiches de l'appelant, triées par {@code dueAt} croissant puis id ; {@code state} par défaut à {@code DUE}. */
    public Page<ReviewItemDto> listMyReviews(CurrentAccountDto learner, PageQuery pageQuery, ReviewState state) {
        var effectiveState = state == null ? ReviewState.DUE : state;
        var filters = new HashMap<String, Object>();
        filters.put("state", effectiveState);
        var fingerprint = CursorCodec.fingerprint(filters);

        OffsetDateTime afterDueAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterDueAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        var now = OffsetDateTime.now(clock);
        Specification<ReviewItem> spec = (root, query, cb) -> cb.equal(root.get("learner").get("id"), learner.id());
        spec = spec.and(switch (effectiveState) {
            case COMPLETED -> (root, query, cb) -> cb.isNotNull(root.get("completedAt"));
            case DUE -> (root, query, cb) -> cb.and(cb.isNull(root.get("completedAt")), cb.lessThanOrEqualTo(root.get("dueAt"), now));
            case SCHEDULED -> (root, query, cb) -> cb.and(cb.isNull(root.get("completedAt")), cb.greaterThan(root.get("dueAt"), now));
        });
        spec = spec.and(KeysetPredicates.afterAscending("dueAt", afterDueAt, afterId));

        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.asc("dueAt"), Sort.Order.asc("id")));
        var rows = reviewItemRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream()
                .map(item -> ReviewItemDto.from(item, clock))
                .toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getDueAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }
}
