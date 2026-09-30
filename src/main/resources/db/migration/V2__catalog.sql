-- Référentiel de thèmes et catalogue d'exercices versionné (versions publiées immuables).

CREATE TABLE topic (
    id         UUID PRIMARY KEY,
    slug       VARCHAR(80)  NOT NULL,
    label      VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_topic_slug UNIQUE (slug)
);

CREATE TABLE exercise (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE exercise_version (
    id                  UUID PRIMARY KEY,
    exercise_id         UUID         NOT NULL REFERENCES exercise (id) ON DELETE CASCADE,
    version_number      INTEGER      NOT NULL,
    title               VARCHAR(200) NOT NULL,
    type                VARCHAR(30)  NOT NULL,
    difficulty          VARCHAR(20)  NOT NULL,
    estimated_minutes   INTEGER      NOT NULL,
    prompt_markdown     VARCHAR(20000) NOT NULL,
    learning_objectives JSONB        NOT NULL,
    files               JSONB,
    technologies        JSONB,
    response_spec       JSONB        NOT NULL,
    hints               JSONB,
    correction          JSONB        NOT NULL,
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_exercise_version_number UNIQUE (exercise_id, version_number)
);

CREATE INDEX idx_exercise_version_exercise ON exercise_version (exercise_id);
CREATE INDEX idx_exercise_version_published ON exercise_version (published_at DESC, id);
CREATE INDEX idx_exercise_version_type ON exercise_version (type);
CREATE INDEX idx_exercise_version_difficulty ON exercise_version (difficulty);

CREATE TABLE exercise_version_topic (
    exercise_version_id UUID NOT NULL REFERENCES exercise_version (id) ON DELETE CASCADE,
    topic_id             UUID NOT NULL REFERENCES topic (id) ON DELETE CASCADE,
    PRIMARY KEY (exercise_version_id, topic_id)
);

CREATE INDEX idx_exercise_version_topic_topic ON exercise_version_topic (topic_id);
