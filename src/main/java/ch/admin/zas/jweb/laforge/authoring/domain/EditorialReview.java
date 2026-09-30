package ch.admin.zas.jweb.laforge.authoring.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Décision d'un relecteur sur un brouillon, à un instant donné. Trace d'audit immuable : une
 * fois enregistrée, une relecture n'est jamais modifiée.
 */
@Entity
@Table(name = "editorial_review")
public class EditorialReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private Draft draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Account reviewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(name = "comment", nullable = false, length = 20000)
    private String comment;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    protected EditorialReview() {
        // Requis par JPA.
    }

    public EditorialReview(Draft draft, Account reviewer, ReviewDecision decision, String comment, OffsetDateTime reviewedAt) {
        this.draft = Objects.requireNonNull(draft, "draft");
        this.reviewer = Objects.requireNonNull(reviewer, "reviewer");
        this.decision = Objects.requireNonNull(decision, "decision");
        this.comment = Objects.requireNonNull(comment, "comment");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt");
    }

    public Draft getDraft() {
        return draft;
    }

    public Account getReviewer() {
        return reviewer;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }
}
