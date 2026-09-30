package ch.admin.zas.jweb.laforge.review.domain;

/**
 * État d'une révision, calculé par rapport à l'heure serveur pour {@code DUE}/{@code SCHEDULED}
 * (voir {@link ReviewItem#state(java.time.OffsetDateTime)}), persisté explicitement pour
 * {@code COMPLETED}.
 */
public enum ReviewState {
    DUE,
    SCHEDULED,
    COMPLETED
}
