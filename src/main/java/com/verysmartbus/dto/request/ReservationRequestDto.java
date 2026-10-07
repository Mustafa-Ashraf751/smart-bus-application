package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;


public record ReservationRequestDto(

        @NotNull(message = "tripId is required")
        Long tripId,

        @NotNull(message = "pickupTripStationId is required")
        Long pickupTripStationId
) {
}
