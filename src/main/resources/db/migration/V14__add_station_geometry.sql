ALTER TABLE stations
    ADD COLUMN location extensions.geometry(Point, 4326);

UPDATE stations
SET location = extensions.ST_SetSRID(
        extensions.ST_MakePoint(longitude, latitude),
        4326
               );

CREATE INDEX idx_stations_location
    ON stations
    USING GIST (location);