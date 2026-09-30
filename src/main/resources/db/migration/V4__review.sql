-- Fiches de révision espacée, liées à leur tentative d'origine.

CREATE TABLE review_item (
    id                  UUID PRIMARY KEY,
    learner_id          UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    exercise_version_id UUID        NOT NULL REFERENCES exercise_version (id),
    exercise_id         UUID        NOT NULL REFERENCES exercise (id),
    source_attempt_id   UUID        NOT NULL REFERENCES attempt (id),
    due_at              TIMESTAMPTZ NOT NULL,
    reason              VARCHAR(20) NOT NULL,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_review_item_learner_due ON review_item (learner_id, due_at, id);
CREATE UNIQUE INDEX uk_review_item_active_per_exercise
    ON review_item (learner_id, exercise_id)
    WHERE completed_at IS NULL;
CREATE UNIQUE INDEX uk_review_item_source_attempt ON review_item (source_attempt_id);
