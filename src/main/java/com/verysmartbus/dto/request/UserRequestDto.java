package com.verysmartbus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UserRequestDto(

        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        String phone,

        String address,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {
    public UserRequestDto {
        if (email != null) email = email.strip().toLowerCase();
        if (name != null) name = name.strip();
    }
}
