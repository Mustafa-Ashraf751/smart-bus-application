package com.verysmartbus.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record TripLocationResponseDto(
        Long tripId,
        MapPointResponseDto currentLocation,
        OffsetDateTime locationUpdatedAt,
        boolean locationStale,
        List<StationPresenceResponseDto> stationsContainingBus
) {
}
