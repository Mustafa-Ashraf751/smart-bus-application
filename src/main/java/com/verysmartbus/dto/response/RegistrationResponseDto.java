package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.UserStatus;

/** Deliberately does not contain a token: a pending user cannot access the application. */
public record RegistrationResponseDto(
        Long userId,
        String username,
        String email,
        UserStatus status,
        String message
) {
}
