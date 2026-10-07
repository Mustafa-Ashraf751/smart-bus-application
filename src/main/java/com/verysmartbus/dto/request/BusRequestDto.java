package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record BusRequestDto(

        @NotBlank(message = "busNumber is required")
        @Size(max = 50, message = "busNumber must be at most 50 characters")
        String busNumber,

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1")
        @Max(value = 200, message = "capacity looks unrealistic (max 200)")
        Integer capacity
) {

    public BusRequestDto {
        if (busNumber != null) {
            busNumber = busNumber.strip();
        }
    }
}
