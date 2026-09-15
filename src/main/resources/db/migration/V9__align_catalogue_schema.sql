-- V9__align_catalogue_schema.sql
--
-- Aligns categories/sunnahs with the client-facing catalogue contract
-- (docs/api-contract.md §1, §3 step 3): a stable human-facing slug on both
-- tables, category status/ordering for the "10 active + 2 coming-soon"
-- product decision, and the sunnahs rename that makes column names match the
-- client-visible field names (action -> reflection, reference -> source).
--
-- Both tables are still empty at this point -- no HTTP surface has existed to
-- write to them yet -- so slug/source can go straight to NOT NULL without a
-- backfill step. Per docs/development/development-workflow.md §8, verify a
-- shape-changing migration against a fresh database
-- (docker compose down -v && docker compose up -d).

ALTER TABLE categories
    ADD COLUMN slug VARCHAR(255) NOT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

ALTER TABLE categories ADD CONSTRAINT categories_slug_key UNIQUE (slug);
ALTER TABLE categories ADD CONSTRAINT chk_categories_status CHECK (status IN ('active', 'coming-soon'));

-- Rename before adding the NOT NULL below, so it lands on the renamed column.
ALTER TABLE sunnahs RENAME COLUMN action TO reflection;
ALTER TABLE sunnahs RENAME COLUMN reference TO source;

ALTER TABLE sunnahs
    ADD COLUMN slug VARCHAR(255) NOT NULL,
    ADD COLUMN arabic_text TEXT,
    ADD COLUMN prompt TEXT,
    ADD COLUMN tags TEXT[] NOT NULL DEFAULT '{}';

ALTER TABLE sunnahs ALTER COLUMN source SET NOT NULL;
ALTER TABLE sunnahs ADD CONSTRAINT sunnahs_slug_key UNIQUE (slug);
