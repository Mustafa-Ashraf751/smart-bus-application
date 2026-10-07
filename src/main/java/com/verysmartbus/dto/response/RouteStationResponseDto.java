package com.verysmartbus.dto.response;

import java.time.OffsetDateTime;
import java.time.LocalTime;

public record RouteStationResponseDto(
        Long id,
        Long routeId,
        Long stationId,
        String stationName,
        Integer stopOrder,
        LocalTime expectedArrivalTime,
        OffsetDateTime createdAt
) {
}
