-- A station is a pickup area (a geofence), not a single GPS coordinate.
ALTER TABLE stations
    ADD COLUMN area extensions.geometry(Polygon, 4326);

-- Preserve existing stations by making a 50-metre area around their old centre point.
UPDATE stations
SET area = extensions.ST_Buffer(location::extensions.geography, 50)::extensions.geometry
WHERE location IS NOT NULL;

-- Spatial index: makes "is this bus inside this station area?" queries fast.
CREATE INDEX idx_stations_area
    ON stations
    USING GIST (area);
