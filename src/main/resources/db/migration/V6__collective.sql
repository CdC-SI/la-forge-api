-- Défis collectifs : inscriptions, code d'invitation haché, discussion.

CREATE TABLE challenge (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(200) NOT NULL,
    exercise_version_id UUID         NOT NULL REFERENCES exercise_version (id),
    creator_id          UUID         NOT NULL REFERENCES account (id),
    closes_at           TIMESTAMPTZ  NOT NULL,
    join_code_hash      VARCHAR(64)  NOT NULL,
    state               VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_challenge_join_code_hash UNIQUE (join_code_hash)
);

CREATE TABLE challenge_participant (
    id           UUID PRIMARY KEY,
    challenge_id UUID        NOT NULL REFERENCES challenge (id) ON DELETE CASCADE,
    account_id   UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    joined_at    TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_challenge_participant UNIQUE (challenge_id, account_id)
);

CREATE TABLE challenge_comment (
    id           UUID PRIMARY KEY,
    challenge_id UUID          NOT NULL REFERENCES challenge (id) ON DELETE CASCADE,
    author_id    UUID          NOT NULL REFERENCES account (id),
    body         VARCHAR(4000) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_challenge_comment_challenge ON challenge_comment (challenge_id, created_at, id);

-- Colonnes ajoutées à `attempt` (défini en V3) pour les requêtes de comparaison des réponses.
CREATE INDEX idx_attempt_challenge_submitted ON attempt (challenge_id, submitted_at, id) WHERE challenge_id IS NOT NULL;
