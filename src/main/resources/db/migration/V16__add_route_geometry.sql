ALTER TABLE routes
    ADD COLUMN path extensions.geometry(LineString, 4326);

CREATE INDEX idx_routes_path
    ON routes
    USING GIST (path);