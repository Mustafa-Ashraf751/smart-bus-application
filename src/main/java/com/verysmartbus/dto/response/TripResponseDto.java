package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.TripStatus;

import java.time.OffsetDateTime;

public record TripResponseDto(
        Long id,
        Long routeId,
        Long busId,
        Long driverId,
        OffsetDateTime scheduledStartTime,
        OffsetDateTime actualStartTime,
        OffsetDateTime actualEndTime,
        MapPointResponseDto currentLocation,
        OffsetDateTime locationUpdatedAt,
        boolean locationStale,
        TripStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
