package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Public registration payload. It deliberately contains no role or permission fields. */
public record RegisterRequestDto(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3 to 50 characters")
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]*$", message = "username may contain lowercase letters, numbers, dots, underscores, and hyphens")
        String username,

        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,

        @Size(max = 255, message = "address must be at most 255 characters")
        String address,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password
) {
    public RegisterRequestDto {
        if (name != null) name = name.strip();
        if (username != null) username = username.strip().toLowerCase(Locale.ROOT);
        if (phone != null) phone = phone.strip();
        if (address != null) address = address.strip();
        if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
    }
}
