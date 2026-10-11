package com.verysmartbus.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TripStationArrivalTimeUpdateRequestDto(
        @NotNull(message = "expectedArrivalTime is required")
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "13:34:44")
        LocalTime expectedArrivalTime
) {
}
