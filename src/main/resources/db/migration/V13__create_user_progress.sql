-- V13__create_user_progress.sql
--
-- Denormalized streak/progress state, one row per user. Kept as a running
-- counter rather than derived by scanning practices on every read: POST
-- /practices updates this row in the same transaction as the practice
-- insert (docs/api-contract.md §2 "Streak"), so GET /me/progress is a cheap
-- single-row lookup and longestStreak never needs a full-history rescan.
--
-- Row is created eagerly at registration -- same "always exists, no
-- lazy-create path" invariant as profiles and notification_preferences.

CREATE TABLE user_progress (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    total_practiced INT NOT NULL DEFAULT 0,
    last_practice_date DATE
);
