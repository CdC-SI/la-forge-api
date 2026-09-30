-- Les anciens brouillons ne sont jamais publies automatiquement.
UPDATE draft
SET state = 'DRAFT', revision = revision + 1, updated_at = CURRENT_TIMESTAMP
WHERE state IN ('IN_REVIEW', 'APPROVED');

ALTER TABLE draft ALTER COLUMN type DROP NOT NULL;
ALTER TABLE draft ALTER COLUMN difficulty DROP NOT NULL;
ALTER TABLE draft ALTER COLUMN estimated_minutes DROP NOT NULL;
ALTER TABLE draft ALTER COLUMN prompt_markdown DROP NOT NULL;
ALTER TABLE draft ALTER COLUMN response_spec DROP NOT NULL;
ALTER TABLE draft ALTER COLUMN correction DROP NOT NULL;

ALTER TABLE draft ADD CONSTRAINT ck_draft_state CHECK (state IN ('DRAFT', 'PUBLISHED'));

-- editorial_review reste une archive historique sans acces applicatif.
