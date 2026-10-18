package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;


public record UserRequestDto(

        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3 to 50 characters")
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]*$", message = "username may contain lowercase letters, numbers, dots, underscores, and hyphens")
        String username,

        String phone,

        String address,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password
) {
    public UserRequestDto {
        if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
        if (name != null) name = name.strip();
        if (username != null) username = username.strip().toLowerCase(Locale.ROOT);
    }
}
