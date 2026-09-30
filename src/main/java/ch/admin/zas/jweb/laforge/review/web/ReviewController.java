package ch.admin.zas.jweb.laforge.review.web;

import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.review.domain.ReviewState;
import ch.admin.zas.jweb.laforge.review.dto.ReviewItemDto;
import ch.admin.zas.jweb.laforge.review.service.ReviewService;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Révisions espacées de l'utilisateur courant (tag {@code Practice}, chemin {@code /me/reviews}). */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/me/reviews")
    public Page<ReviewItemDto> listMyReviews(
            @CurrentAccount Account account,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) ReviewState state) {
        return reviewService.listMyReviews(account, PageQuery.of(limit, cursor), state);
    }
}
