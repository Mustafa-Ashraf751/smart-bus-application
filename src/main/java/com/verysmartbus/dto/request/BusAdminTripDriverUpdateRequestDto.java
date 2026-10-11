package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;

public record BusAdminTripDriverUpdateRequestDto(
        @NotNull(message = "driverId is required")
        Long driverId
) {
}
