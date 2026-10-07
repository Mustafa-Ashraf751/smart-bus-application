-- V11__create_transport_preference.sql

CREATE TABLE transport_preference (
    preference_id   BIGSERIAL PRIMARY KEY,

    user_id         BIGINT NOT NULL,
    route_id        BIGINT NOT NULL,
    station_id      BIGINT NOT NULL,

    direction       VARCHAR(20) NOT NULL,
    preferred_time  TIME NOT NULL,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transport_preference_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transport_preference_route
        FOREIGN KEY (route_id) REFERENCES routes (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transport_preference_station
        FOREIGN KEY (station_id) REFERENCES stations (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_transport_preference_direction
        CHECK (direction IN ('TO_COMPANY', 'FROM_COMPANY')),

    CONSTRAINT uq_transport_preference_user_direction
        UNIQUE (user_id, direction)
);

CREATE INDEX idx_transport_preference_user_id ON transport_preference(user_id);
CREATE INDEX idx_transport_preference_route_id ON transport_preference(route_id);
CREATE INDEX idx_transport_preference_station_id ON transport_preference(station_id);
