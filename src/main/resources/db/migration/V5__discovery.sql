-- Fiches de veille technique, publiées directement (pas de cycle brouillon).

CREATE TABLE article (
    id            UUID PRIMARY KEY,
    title         VARCHAR(200)   NOT NULL,
    summary       VARCHAR(2000)  NOT NULL,
    body_markdown VARCHAR(20000) NOT NULL,
    technologies  JSONB,
    sources       JSONB          NOT NULL,
    published_at  TIMESTAMPTZ    NOT NULL,
    revision      INTEGER        NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_article_published ON article (published_at DESC, id);

CREATE TABLE article_topic (
    article_id UUID NOT NULL REFERENCES article (id) ON DELETE CASCADE,
    topic_id   UUID NOT NULL REFERENCES topic (id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, topic_id)
);

CREATE TABLE article_related_exercise_version (
    article_id          UUID NOT NULL REFERENCES article (id) ON DELETE CASCADE,
    exercise_version_id UUID NOT NULL REFERENCES exercise_version (id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, exercise_version_id)
);
