package com.verysmartbus.dto.request;

import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.entity.enums.Direction;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record RouteRequestDto(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "status is required")
        RouteStatus status,

        @NotNull(message = "direction is required")
        Direction direction,

        @NotNull(message = "defaultDepartureTime is required")
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "07:30:00")
        LocalTime defaultDepartureTime,

        @NotNull(message = "defaultBusId is required")
        Long defaultBusId,

        @NotNull(message = "defaultDriverId is required")
        Long defaultDriverId,

        @NotNull(message = "defaultBusAdminId is required")
        Long defaultBusAdminId
) {
    public RouteRequestDto {
        if (name != null) {
            name = name.strip();
        }
    }
}
