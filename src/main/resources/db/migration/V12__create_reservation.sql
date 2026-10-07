-- V12__create_reservation.sql
CREATE TABLE reservation (
    reservation_id          BIGSERIAL PRIMARY KEY,

    user_id                 BIGINT NOT NULL,
    trip_id                 BIGINT NOT NULL,
    pickup_trip_station_id  BIGINT NOT NULL,

    status                  VARCHAR(20) NOT NULL DEFAULT 'RESERVED',

    reserved_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    cancelled_at            TIMESTAMP NULL,

    CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reservation_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE CASCADE,

    
    CONSTRAINT fk_reservation_pickup_trip_station
        FOREIGN KEY (pickup_trip_station_id) REFERENCES trip_stations(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'CANCELLED', 'COMPLETED')),

    CONSTRAINT uq_reservation_user_trip
        UNIQUE (user_id, trip_id)

);

CREATE INDEX idx_reservation_user_id ON reservation(user_id);
CREATE INDEX idx_reservation_trip_id ON reservation(trip_id);
CREATE INDEX idx_reservation_pickup_trip_station_id ON reservation(pickup_trip_station_id);
CREATE INDEX idx_reservation_status ON reservation(status);
