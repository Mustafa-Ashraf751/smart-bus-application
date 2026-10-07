package com.verysmartbus.dto.response;

import java.time.OffsetDateTime;

public record TripStationResponseDto(
        Long id,
        Long tripId,
        Long stationId,
        String stationName,
        Integer stopOrder,
        OffsetDateTime createdAt
) {
}
