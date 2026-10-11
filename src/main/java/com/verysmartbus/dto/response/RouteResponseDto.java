package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.entity.enums.Direction;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record RouteResponseDto(
        Long id,
        String name,
        Direction direction,
        List<MapPointResponseDto> path,
        RouteStatus status,
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "07:30:00")
        LocalTime defaultDepartureTime,
        Long defaultBusId,
        Long defaultDriverId,
        Long defaultBusAdminId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
