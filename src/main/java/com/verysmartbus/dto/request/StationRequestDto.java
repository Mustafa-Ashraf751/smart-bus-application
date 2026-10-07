package com.verysmartbus.dto.request;

import com.verysmartbus.entity.enums.StationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StationRequestDto(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "address must be at most 255 characters")
        String address,

        @NotNull(message = "pickupLatitude is required")
        @DecimalMin(value = "-90.0", message = "pickupLatitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "pickupLatitude must be between -90 and 90")
        BigDecimal pickupLatitude,

        @NotNull(message = "pickupLongitude is required")
        @DecimalMin(value = "-180.0", message = "pickupLongitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "pickupLongitude must be between -180 and 180")
        BigDecimal pickupLongitude,

        @NotNull(message = "status is required")
        StationStatus status
) {
    public StationRequestDto {
        if (name != null) {
            name = name.strip();
        }
        if (address != null) {
            address = address.strip();
        }
    }
}
