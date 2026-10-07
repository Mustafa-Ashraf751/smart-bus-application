
ALTER TABLE transport_preference
    DROP COLUMN preferred_time;

ALTER TABLE transport_preference
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;