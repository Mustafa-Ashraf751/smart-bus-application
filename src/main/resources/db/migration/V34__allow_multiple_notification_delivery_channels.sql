-- V30 named the original one-column unique constraint explicitly. V33 introduced
-- per-channel jobs, so remove that legacy constraint while keeping the V33
-- (event_key, delivery_channel) uniqueness rule.
ALTER TABLE notification_outbox
    DROP CONSTRAINT IF EXISTS uq_notification_outbox_event_key;
