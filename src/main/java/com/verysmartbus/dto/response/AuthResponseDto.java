package com.verysmartbus.dto.response;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
