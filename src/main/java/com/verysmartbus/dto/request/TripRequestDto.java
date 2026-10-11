package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TripRequestDto(
        @NotNull(message = "routeId is required")
        Long routeId,

        @NotNull(message = "serviceDate is required")
        LocalDate serviceDate
) {
}
