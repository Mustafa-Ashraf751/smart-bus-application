-- A notification may be delivered through more than one channel. Existing rows were
-- WebSocket deliveries, so preserve them with that explicit default.
ALTER TABLE notification_outbox
    ADD COLUMN delivery_channel VARCHAR(20) NOT NULL DEFAULT 'WEBSOCKET',
    ADD COLUMN recipient_email VARCHAR(150);

ALTER TABLE notification_outbox
    DROP CONSTRAINT IF EXISTS notification_outbox_event_key_key;

ALTER TABLE notification_outbox
    ADD CONSTRAINT uq_notification_outbox_event_channel
        UNIQUE (event_key, delivery_channel),
    ADD CONSTRAINT chk_notification_outbox_delivery_channel
        CHECK (delivery_channel IN ('WEBSOCKET', 'EMAIL'));

CREATE INDEX idx_notification_outbox_channel_pending
    ON notification_outbox(delivery_channel, status, next_attempt_at, created_at);
