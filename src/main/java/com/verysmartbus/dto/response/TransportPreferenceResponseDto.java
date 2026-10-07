package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.Direction;
import com.verysmartbus.entity.TransportPreference;

import java.time.LocalDateTime;


public record TransportPreferenceResponseDto(
        Long preferenceId,
        Long userId,
        Long routeId,
        String routeName,
        Long stationId,
        String stationName,
        Direction direction,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TransportPreferenceResponseDto fromEntity(TransportPreference entity) {
        return new TransportPreferenceResponseDto(
                entity.getPreferenceId(),
                entity.getUser().getId(),
                entity.getRoute().getId(),
                entity.getRoute().getName(),
                entity.getStation().getId(),
                entity.getStation().getName(),
                entity.getDirection(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
