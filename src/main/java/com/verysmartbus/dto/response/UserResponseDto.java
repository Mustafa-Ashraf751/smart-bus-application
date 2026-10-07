package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

// password / passwordHash are deliberately absent — never sent back to any client.
public record UserResponseDto(
        Long userId,
        String name,
        String phone,
        String address,
        String email,
        UserStatus status,
        Set<String> roleNames,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
