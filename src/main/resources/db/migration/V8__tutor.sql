-- Historique des échanges avec le tuteur assisté par IA (persistés uniquement en cas de succès).

CREATE TABLE tutor_exchange (
    id              UUID           PRIMARY KEY,
    attempt_id      UUID           NOT NULL REFERENCES attempt (id) ON DELETE CASCADE,
    question        VARCHAR(4000)  NOT NULL,
    answer_markdown VARCHAR(20000) NOT NULL,
    sources         JSONB,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_tutor_exchange_attempt ON tutor_exchange (attempt_id, created_at, id);
