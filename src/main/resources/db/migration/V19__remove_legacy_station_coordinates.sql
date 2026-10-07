ALTER TABLE stations
    ALTER COLUMN area SET NOT NULL;

DROP INDEX idx_stations_location;

ALTER TABLE stations
    DROP COLUMN location,
    DROP COLUMN latitude,
    DROP COLUMN longitude;
