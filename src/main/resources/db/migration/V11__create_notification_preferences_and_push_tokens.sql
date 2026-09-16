-- V11__create_notification_preferences_and_push_tokens.sql
--
-- Storage for how a user wants to be reminded, and where to push to. Nothing
-- reads these yet: the scheduled job that would check preferences against
-- practice history and call Expo's push API is deferred to a future stretch
-- phase (docs/api-contract.md §5). This migration only lands the tables the
-- Phase 4 endpoints read and write.
--
-- notification_preferences is keyed directly on user_id (one row per user,
-- like profiles) rather than a separate generated id -- NotificationPreferencesService
-- creates the row eagerly at registration time, mirroring how ProfileService
-- guarantees a profile row exists for every user.

CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    daily_reminder BOOLEAN NOT NULL DEFAULT true,
    streak_reminder BOOLEAN NOT NULL DEFAULT true,
    weekly_summary BOOLEAN NOT NULL DEFAULT false,
    reminder_time TIME NOT NULL DEFAULT '09:00:00'
);

CREATE TABLE push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    expo_push_token VARCHAR(255) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE push_tokens ADD CONSTRAINT push_tokens_user_id_expo_push_token_key UNIQUE (user_id, expo_push_token);
