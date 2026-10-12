package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Both station ids are IDs from the trip_stations table (not stations, not route_stations).
 */
public record ReservationRequestDto(
        @NotNull Long tripId,
        @NotNull Long pickupTripStationId,
        @NotNull Long dropoffTripStationId
) {
}
