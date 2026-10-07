package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotNull;

public record BusAdminTripBusUpdateRequestDto(
        @NotNull(message = "busId is required")
        Long busId
) {
}
