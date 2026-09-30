-- Déduplication des requêtes mutatives porteuses de l'en-tête Idempotency-Key (voir
-- ch.admin.zas.jweb.laforge.common.idempotency). La clé n'est unique que combinée au compte,
-- à la méthode HTTP et au chemin.

CREATE TABLE idempotency_record (
    id              UUID        PRIMARY KEY,
    idempotency_key UUID        NOT NULL,
    account_id      UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    http_method     VARCHAR(10) NOT NULL,
    path            VARCHAR(500) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotency_record_scope UNIQUE (idempotency_key, account_id, http_method, path)
);

CREATE INDEX idx_idempotency_record_expires_at ON idempotency_record (expires_at);
