package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequestDto(

        @NotBlank(message = "name is required")
        @Size(max = 50)
        String name,

        @Size(max = 255)
        String description
) {
    public RoleRequestDto {
        if (name != null) name = name.strip();
    }
}
