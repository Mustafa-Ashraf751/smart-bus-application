package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String message,
        String payloadJson,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
