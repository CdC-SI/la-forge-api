-- Tentatives de pratique et indices révélés.

CREATE TABLE attempt (
    id                     UUID PRIMARY KEY,
    learner_id             UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    exercise_version_id    UUID        NOT NULL REFERENCES exercise_version (id),
    challenge_id           UUID,
    review_item_id         UUID,
    status                 VARCHAR(20) NOT NULL,
    started_at             TIMESTAMPTZ NOT NULL,
    submitted_at           TIMESTAMPTZ,
    answer                 JSONB,
    objective_result       VARCHAR(20),
    self_assessment_mastery VARCHAR(20),
    self_assessment_note   VARCHAR(2000),
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_attempt_learner ON attempt (learner_id, started_at DESC, id);
CREATE INDEX idx_attempt_exercise_version ON attempt (exercise_version_id);
CREATE INDEX idx_attempt_challenge ON attempt (challenge_id);
CREATE INDEX idx_attempt_review_item ON attempt (review_item_id);

CREATE TABLE attempt_revealed_hint (
    attempt_id UUID    NOT NULL REFERENCES attempt (id) ON DELETE CASCADE,
    hint_level INTEGER NOT NULL,
    PRIMARY KEY (attempt_id, hint_level)
);
