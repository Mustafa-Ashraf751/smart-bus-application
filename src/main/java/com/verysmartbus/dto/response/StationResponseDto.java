package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.StationStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record StationResponseDto(
        Long id,
        String name,
        String address,
        List<MapPointResponseDto> areaBoundary,
        StationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
