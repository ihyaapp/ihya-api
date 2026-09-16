-- V14__create_notifications.sql
--
-- The in-app notification feed (docs/api-contract.md §2 "Notifications"). The
-- only producer in v1 is a milestone_earned row, inserted inline by
-- PracticeService in the same transaction as the practice that unlocked it --
-- no scheduler yet (see docs/api-contract.md §5).
--
-- read_at is nullable rather than a boolean flag: null means unread, a
-- timestamp means read (and records when). id gets its own generated column
-- (not composite-keyed like daily_assignments) since the feed is paginated
-- and addressed by id via POST /notifications/read's optional ids list.

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(32) NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_id_created_at ON notifications (user_id, created_at DESC);
