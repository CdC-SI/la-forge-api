-- Comptes, rôles cumulatifs, jetons de vérification et de renouvellement.

CREATE TABLE account (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(200) NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_account_email UNIQUE (email)
);

CREATE TABLE account_role (
    account_id UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,
    PRIMARY KEY (account_id, role)
);

CREATE TABLE verification_token (
    id          UUID PRIMARY KEY,
    account_id  UUID         NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_verification_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_verification_token_account ON verification_token (account_id);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    account_id  UUID         NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_account ON refresh_token (account_id);
