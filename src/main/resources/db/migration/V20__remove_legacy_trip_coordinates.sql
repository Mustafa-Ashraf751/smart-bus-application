ALTER TABLE trips
    DROP CONSTRAINT chk_trips_location_state,
    DROP CONSTRAINT chk_trips_current_latitude,
    DROP CONSTRAINT chk_trips_current_longitude,
    DROP COLUMN current_latitude,
    DROP COLUMN current_longitude;
