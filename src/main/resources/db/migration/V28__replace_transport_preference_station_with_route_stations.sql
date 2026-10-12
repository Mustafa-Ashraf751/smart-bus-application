-- Preference now points at two RouteStations (pickup + drop-off) instead of one Station.
-- Non-destructive: old rows keep their data. station_id becomes nullable and is no longer
-- mapped by the entity. Drop it, and make the new columns NOT NULL, in a later migration
-- once old preferences are cleaned up or re-entered by their users.
ALTER TABLE transport_preference
    ADD COLUMN pickup_route_station_id  BIGINT,
    ADD COLUMN dropoff_route_station_id BIGINT;

ALTER TABLE transport_preference
    ALTER COLUMN station_id DROP NOT NULL;

ALTER TABLE transport_preference
    ADD CONSTRAINT fk_transport_preference_pickup_route_station
        FOREIGN KEY (pickup_route_station_id) REFERENCES route_stations (id),
    ADD CONSTRAINT fk_transport_preference_dropoff_route_station
        FOREIGN KEY (dropoff_route_station_id) REFERENCES route_stations (id);
