package com.verysmartbus.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoutePathRequestDto(
        @NotEmpty(message = "path points are required")
        @Size(min = 2, message = "a route path requires at least two points")
        List<@NotNull(message = "path point is required") @Valid RoutePathPointRequestDto> points
) {
}
