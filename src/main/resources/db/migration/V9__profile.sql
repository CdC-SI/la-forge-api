-- Préférences de pratique (créées paresseusement, une par compte).

CREATE TABLE preferences (
    id              UUID        PRIMARY KEY,
    account_id      UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    stack           JSONB,
    difficulty      VARCHAR(20) NOT NULL,
    session_minutes INTEGER     NOT NULL,
    locale          VARCHAR(5)  NOT NULL,
    time_zone       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_preferences_account UNIQUE (account_id)
);

CREATE TABLE preferences_topic (
    preferences_id UUID NOT NULL REFERENCES preferences (id) ON DELETE CASCADE,
    topic_id       UUID NOT NULL REFERENCES topic (id) ON DELETE CASCADE,
    PRIMARY KEY (preferences_id, topic_id)
);

CREATE INDEX idx_preferences_topic_topic ON preferences_topic (topic_id);
