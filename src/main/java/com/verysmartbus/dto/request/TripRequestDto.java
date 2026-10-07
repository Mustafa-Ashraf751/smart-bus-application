package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TripRequestDto(
        @NotNull(message = "routeId is required")
        Long routeId,

        @NotNull(message = "busId is required")
        Long busId,

        @NotNull(message = "driverId is required")
        Long driverId,

        @NotNull(message = "serviceDate is required")
        LocalDate serviceDate,

        @NotNull(message = "scheduledStartTime is required")
        OffsetDateTime scheduledStartTime
) {
}
