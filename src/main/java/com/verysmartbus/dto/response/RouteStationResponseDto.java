package com.verysmartbus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.time.LocalTime;

public record RouteStationResponseDto(
        Long id,
        Long routeId,
        Long stationId,
        String stationName,
        Integer stopOrder,
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "13:34:44")
        LocalTime expectedArrivalTime,
        OffsetDateTime createdAt
) {
}
