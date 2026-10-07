package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TripStationRequestDto(
        @NotNull(message = "tripId is required")
        Long tripId,

        @NotNull(message = "stationId is required")
        Long stationId,

        @NotNull(message = "stopOrder is required")
        @Min(value = 1, message = "stopOrder must be at least 1")
        Integer stopOrder
) {
}
