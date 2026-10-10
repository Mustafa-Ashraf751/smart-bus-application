-- A Trip is one permanent daily execution. It must never be reset and reused.
ALTER TABLE trips
    ADD COLUMN service_date DATE;

-- The scheduled time is the only reliable historical date for an existing Trip.
UPDATE trips
SET service_date = (scheduled_start_time AT TIME ZONE 'UTC')::DATE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM trips
        GROUP BY route_id, service_date
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot add unique route/service-date rule: duplicate trips already exist. Resolve the duplicate records before applying V26.';
    END IF;
END $$;

ALTER TABLE trips
    ALTER COLUMN service_date SET NOT NULL,
    ADD CONSTRAINT uq_trips_route_service_date UNIQUE (route_id, service_date),
    DROP COLUMN active_date;
