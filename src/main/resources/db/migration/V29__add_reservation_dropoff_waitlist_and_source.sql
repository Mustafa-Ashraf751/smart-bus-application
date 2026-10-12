DROP INDEX IF EXISTS uq_reservation_one_active_per_user;


ALTER TABLE reservation
    ADD COLUMN dropoff_trip_station_id BIGINT,
    ADD COLUMN source                  VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_dropoff_trip_station
        FOREIGN KEY (dropoff_trip_station_id) REFERENCES trip_stations (id)
            ON DELETE RESTRICT,
    ADD CONSTRAINT chk_reservation_source
        CHECK (source IN ('MANUAL', 'PREFERENCE'));


DO
$$
DECLARE
old_check RECORD;
BEGIN
FOR old_check IN
SELECT conname
FROM pg_constraint
WHERE conrelid = 'reservation'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
          AND pg_get_constraintdef(oid) NOT ILIKE '%cancelled_at%'
        LOOP
            EXECUTE format('ALTER TABLE reservation DROP CONSTRAINT %I', old_check.conname);
END LOOP;
END
$$;

ALTER TABLE reservation
    ADD CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'WAITLISTED', 'CANCELLED', 'COMPLETED'));


CREATE UNIQUE INDEX uq_reservation_user_trip_active
    ON reservation (user_id, trip_id)
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_reservation_trip_status
    ON reservation (trip_id, status);
