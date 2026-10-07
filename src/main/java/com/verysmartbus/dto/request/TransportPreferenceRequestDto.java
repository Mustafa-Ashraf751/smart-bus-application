package com.verysmartbus.dto.request;

import com.verysmartbus.entity.enums.Direction;
import jakarta.validation.constraints.NotNull;


public record TransportPreferenceRequestDto(

        @NotNull(message = "routeId is required")
        Long routeId,

        @NotNull(message = "stationId is required")
        Long stationId,

        @NotNull(message = "direction is required")
        Direction direction
) {
}
