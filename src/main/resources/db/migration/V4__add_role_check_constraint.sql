-- V4__add_role_check_constraint.sql
--
-- Database-level guard for users.role. The Role enum mapping
-- (@Enumerated(EnumType.STRING)) already stops the application from writing
-- anything but 'USER' / 'ADMIN'; this constraint covers the one path that
-- bypasses the app -- a manual/direct SQL update, e.g. provisioning the first
-- admin account. Named so it can be located and altered if a third role value
-- is ever introduced.

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
