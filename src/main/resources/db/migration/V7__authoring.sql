-- Brouillons d'exercices et relectures éditoriales (machine à états DRAFT → IN_REVIEW → APPROVED → PUBLISHED).

CREATE TABLE draft (
    id                  UUID PRIMARY KEY,
    exercise_id         UUID           NOT NULL REFERENCES exercise (id),
    author_id           UUID           NOT NULL REFERENCES account (id),
    state               VARCHAR(20)    NOT NULL,
    revision            INTEGER        NOT NULL,
    base_version        INTEGER        NOT NULL,
    published_version   INTEGER,
    title               VARCHAR(200)   NOT NULL,
    type                VARCHAR(30)    NOT NULL,
    difficulty          VARCHAR(20)    NOT NULL,
    estimated_minutes   INTEGER        NOT NULL,
    prompt_markdown     VARCHAR(20000) NOT NULL,
    learning_objectives JSONB          NOT NULL,
    files               JSONB,
    technologies        JSONB,
    response_spec       JSONB          NOT NULL,
    hints               JSONB,
    correction          JSONB          NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_draft_exercise ON draft (exercise_id);
CREATE INDEX idx_draft_author ON draft (author_id);
CREATE INDEX idx_draft_state ON draft (state);

-- Une seule révision non publiée par exercice.
CREATE UNIQUE INDEX uk_draft_exercise_unpublished ON draft (exercise_id) WHERE state <> 'PUBLISHED';

CREATE TABLE draft_topic (
    draft_id UUID NOT NULL REFERENCES draft (id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES topic (id) ON DELETE CASCADE,
    PRIMARY KEY (draft_id, topic_id)
);

CREATE INDEX idx_draft_topic_topic ON draft_topic (topic_id);

CREATE TABLE editorial_review (
    id          UUID PRIMARY KEY,
    draft_id    UUID          NOT NULL REFERENCES draft (id) ON DELETE CASCADE,
    reviewer_id UUID          NOT NULL REFERENCES account (id),
    decision    VARCHAR(20)   NOT NULL,
    comment     VARCHAR(20000) NOT NULL,
    reviewed_at TIMESTAMPTZ   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_editorial_review_draft ON editorial_review (draft_id, reviewed_at);
