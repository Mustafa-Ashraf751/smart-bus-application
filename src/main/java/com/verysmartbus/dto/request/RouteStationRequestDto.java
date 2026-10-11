package com.verysmartbus.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "13:34:44")
        LocalTime expectedArrivalTime
) {
}
