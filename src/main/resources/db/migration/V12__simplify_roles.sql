-- Les attributions existantes gagnent AUTHOR, sans doublon pour les comptes déjà auteurs.
INSERT INTO account_role (account_id, role)
SELECT account_id, 'AUTHOR' FROM account_role WHERE role = 'REVIEWER'
ON CONFLICT (account_id, role) DO NOTHING;

DELETE FROM account_role WHERE role = 'REVIEWER';

-- Toute mutation administrative des rôles verrouille cette ligne jusqu'au commit.
CREATE TABLE account_role_lock (
    id INTEGER PRIMARY KEY,
    CONSTRAINT ck_account_role_lock_singleton CHECK (id = 1)
);

INSERT INTO account_role_lock (id) VALUES (1);
