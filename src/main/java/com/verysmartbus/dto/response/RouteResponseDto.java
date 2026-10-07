package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.RouteStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record RouteResponseDto(
        Long id,
        String name,
        List<MapPointResponseDto> path,
        RouteStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
