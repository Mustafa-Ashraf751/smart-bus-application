package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRequestDto(

        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description
) {
    public PermissionRequestDto {
        if (name != null) name = name.strip();
    }
}
