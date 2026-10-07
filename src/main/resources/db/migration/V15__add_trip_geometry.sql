ALTER TABLE trips
    ADD COLUMN current_location extensions.geometry(Point, 4326);

UPDATE trips
SET current_location = extensions.ST_SetSRID(
        extensions.ST_MakePoint(
                current_longitude,
                current_latitude
        ),
        4326
                       )
WHERE current_latitude IS NOT NULL
  AND current_longitude IS NOT NULL;

CREATE INDEX idx_trips_current_location
    ON trips
    USING GIST (current_location);