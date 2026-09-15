-- V7__extend_me.sql
--
-- Extends the "me" surface: per-user timezone, a dismissible personalize
-- prompt flag on the profile, and the user's chosen interest categories.
--
-- category_slug has no FK to categories yet because categories are currently
-- keyed by UUID id, not slug — there is no slug column on categories to
-- reference. This table stores the slug as a plain string until that's added.

ALTER TABLE users ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';

ALTER TABLE profiles ADD COLUMN personalize_prompt_dismissed BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE user_interests (
    user_id UUID NOT NULL REFERENCES users(id),
    category_slug VARCHAR(255) NOT NULL,

    PRIMARY KEY (user_id, category_slug)
);
