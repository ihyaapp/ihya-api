-- V3__add_role_to_users.sql
--
-- Foundation for Week 2 role-based authorization: catalogue reads stay open to
-- any authenticated user, catalogue writes become ADMIN-only.
--
-- Stored as text to match User.role (@Enumerated(EnumType.STRING)). NOT NULL
-- with DEFAULT 'USER' backfills every existing row and makes 'USER' the value
-- for any future insert that omits the column. The first admin is provisioned
-- with a manual UPDATE, outside the application.

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
