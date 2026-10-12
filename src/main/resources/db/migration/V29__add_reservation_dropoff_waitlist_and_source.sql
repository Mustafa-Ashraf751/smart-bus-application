-- Reservations are now per daily trip, gain a drop-off stop, a WAITLISTED state and a source.

-- V23 allowed only ONE non-cancelled reservation per user, ever. That no longer holds:
-- a user has one reservation per daily trip. Replaced below by one non-cancelled
-- reservation per (user, trip).
DROP INDEX IF EXISTS uq_reservation_one_active_per_user;

-- dropoff_trip_station_id stays nullable: old reservations have no drop-off and one cannot be
-- invented. Tighten to NOT NULL in a later migration once legacy rows are cleaned up.
ALTER TABLE reservation
    ADD COLUMN dropoff_trip_station_id BIGINT,
    ADD COLUMN source                  VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_dropoff_trip_station
        FOREIGN KEY (dropoff_trip_station_id) REFERENCES trip_stations (id)
            ON DELETE RESTRICT,
    ADD CONSTRAINT chk_reservation_source
        CHECK (source IN ('MANUAL', 'PREFERENCE'));

-- Replace the status check (whatever it is called) but keep chk_reservation_cancelled_at,
-- which also mentions "status" and is excluded by the cancelled_at filter.
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

-- COMPLETED is kept so existing rows and any code that uses it keep working.
ALTER TABLE reservation
    ADD CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'WAITLISTED', 'CANCELLED', 'COMPLETED'));

-- One live (RESERVED or WAITLISTED) reservation per user per trip. A cancelled one may
-- coexist with a new booking of the same trip.
CREATE UNIQUE INDEX uq_reservation_user_trip_active
    ON reservation (user_id, trip_id)
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_reservation_trip_status
    ON reservation (trip_id, status);
