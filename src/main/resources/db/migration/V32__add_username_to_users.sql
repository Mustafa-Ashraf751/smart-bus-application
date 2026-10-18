-- A display name is not a login identity. Add a separate, case-insensitively unique username.
ALTER TABLE users
    ADD COLUMN username VARCHAR(50);

-- Existing accounts can keep signing in with email. Give each one a deterministic, valid
-- username that an administrator may later replace with a friendlier one.
UPDATE users
SET username = 'user-' || user_id
WHERE username IS NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX uq_users_username_lower
    ON users (lower(username));

ALTER TABLE users
    ADD CONSTRAINT chk_users_username_format
    CHECK (username ~ '^[a-z0-9][a-z0-9._-]{2,49}$');
