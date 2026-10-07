package com.verysmartbus.dto.request;

import com.verysmartbus.entity.enums.RouteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RouteRequestDto(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "status is required")
        RouteStatus status
) {
    public RouteRequestDto {
        if (name != null) {
            name = name.strip();
        }
    }
}
