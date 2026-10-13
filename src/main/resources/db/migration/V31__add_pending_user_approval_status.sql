-- Public registrations wait for an administrator; existing users keep their current status.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE'));
