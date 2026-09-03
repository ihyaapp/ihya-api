-- V5__create_categories_table.sql
--
-- First table of the Sunnah content catalogue. Category only; the Sunnah table
-- and its FK to categories are added in a later task.
--
-- Mirrors the users table conventions from V2: UUID PK with a gen_random_uuid()
-- default, TIMESTAMPTZ created_at defaulting to now(). Category.id uses
-- GenerationType.UUID, so Hibernate supplies the id on insert; the column
-- default only covers direct SQL inserts.

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
