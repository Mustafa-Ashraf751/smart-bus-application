
ALTER TABLE reservation
    DROP CONSTRAINT uq_reservation_user_trip;

    CREATE UNIQUE INDEX IF NOT EXISTS uq_reservation_one_active_per_user
        ON reservation (user_id)
        WHERE status <> 'CANCELLED';