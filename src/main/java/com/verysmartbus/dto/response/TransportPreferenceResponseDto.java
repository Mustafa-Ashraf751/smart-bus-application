package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.Direction;

import java.time.LocalDateTime;

public record TransportPreferenceResponseDto(
        Long preferenceId,
        Long userId,
        Long routeId,
        String routeName,
        Long pickupRouteStationId,
        String pickupStationName,
        Long dropoffRouteStationId,
        String dropoffStationName,
        Direction direction,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
