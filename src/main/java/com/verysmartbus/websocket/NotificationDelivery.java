package com.verysmartbus.websocket;

import com.verysmartbus.dto.response.NotificationResponseDto;

public record NotificationDelivery(
        String recipientUsername,
        NotificationResponseDto notification
) {
}
