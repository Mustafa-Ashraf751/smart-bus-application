-- Route defaults are copied into each new daily Trip. Existing routes remain
-- configurable: they must be completed by an admin before a new Trip is created.
ALTER TABLE routes
    ADD COLUMN direction VARCHAR(20),
    ADD COLUMN default_departure_time TIME,
    ADD COLUMN default_bus_id BIGINT,
    ADD COLUMN default_driver_id BIGINT,
    ADD COLUMN default_bus_admin_id BIGINT,
    ADD CONSTRAINT chk_routes_direction
        CHECK (direction IS NULL OR direction IN ('TO_COMPANY', 'FROM_COMPANY')),
    ADD CONSTRAINT fk_routes_default_bus
        FOREIGN KEY (default_bus_id) REFERENCES buses (id),
    ADD CONSTRAINT fk_routes_default_driver
        FOREIGN KEY (default_driver_id) REFERENCES users (user_id),
    ADD CONSTRAINT fk_routes_default_bus_admin
        FOREIGN KEY (default_bus_admin_id) REFERENCES users (user_id);

ALTER TABLE trips
    ADD COLUMN bus_admin_id BIGINT,
    ADD CONSTRAINT fk_trips_bus_admin
        FOREIGN KEY (bus_admin_id) REFERENCES users (user_id);

INSERT INTO role (name, description)
VALUES ('BUS_ADMIN', 'Operates assigned routes and daily trips')
ON CONFLICT (name) DO NOTHING;
