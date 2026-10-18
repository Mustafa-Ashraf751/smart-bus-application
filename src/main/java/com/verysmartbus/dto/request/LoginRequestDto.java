package com.verysmartbus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequestDto(
        @NotBlank(message = "username or email is required")
        @Size(max = 150, message = "username or email must be at most 150 characters")
        String identifier,

        @NotBlank(message = "password is required")
        @Size(max = 72, message = "password must be at most 72 characters")
        String password
) {
    public LoginRequestDto {
        if (identifier != null) {
            identifier = identifier.strip().toLowerCase(Locale.ROOT);
        }
    }
}
