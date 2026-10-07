package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record RouteStationRequestDto(
        @NotNull(message = "routeId is required")
        Long routeId,

        @NotNull(message = "stationId is required")
        Long stationId,

        @NotNull(message = "stopOrder is required")
        @Min(value = 1, message = "stopOrder must be at least 1")
        Integer stopOrder,

        @NotNull(message = "expectedArrivalTime is required")
        LocalTime expectedArrivalTime
) {
}
