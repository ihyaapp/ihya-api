-- V12__create_daily_practice_tables.sql
--
-- The core habit loop: one assigned Sunnah per user per local day, and the
-- completion record when they mark it done.
--
-- daily_assignments is keyed on (user_id, assignment_date) directly -- there
-- is exactly one assignment per user per day by definition, so the composite
-- key doubles as the "already assigned today?" existence check GET
-- /assignment/today relies on.
--
-- practices gets its own generated id (it's paginated and addressed by id in
-- PATCH /practices/{id}), with a separate UNIQUE(user_id, practice_date) --
-- that's the constraint POST /practices targets with
-- INSERT ... ON CONFLICT (user_id, practice_date) DO NOTHING for idempotent,
-- race-safe one-practice-per-day writes.

CREATE TABLE daily_assignments (
    user_id UUID NOT NULL REFERENCES users(id),
    assignment_date DATE NOT NULL,
    sunnah_id UUID NOT NULL REFERENCES sunnahs(id),
    replacement_used BOOLEAN NOT NULL DEFAULT false,
    replacement_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, assignment_date)
);

CREATE TABLE practices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    sunnah_id UUID NOT NULL REFERENCES sunnahs(id),
    practice_date DATE NOT NULL,
    feeling TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE practices ADD CONSTRAINT practices_user_id_practice_date_key UNIQUE (user_id, practice_date);

CREATE INDEX idx_practices_user_id_practice_date ON practices (user_id, practice_date DESC);
