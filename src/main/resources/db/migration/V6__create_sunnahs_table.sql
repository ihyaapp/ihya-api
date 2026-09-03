-- V6__create_sunnahs_table.sql
--
-- Second catalogue table: individual Sunnah practices, each belonging to exactly
-- one Category. Same conventions as categories (V5) / users (V2): UUID PK with a
-- gen_random_uuid() default, TIMESTAMPTZ created_at defaulting to now().
--
-- description and action carry real guidance content (not short labels), so they
-- are TEXT. title and reference are short, so they stay VARCHAR(255).
-- category_id is a mandatory FK to categories; the entity maps it LAZY.

CREATE TABLE sunnahs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES categories(id),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    action TEXT NOT NULL,
    reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
