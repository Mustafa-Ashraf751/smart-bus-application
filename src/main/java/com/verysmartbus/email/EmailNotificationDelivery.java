package com.verysmartbus.email;

import com.verysmartbus.entity.enums.NotificationType;

/** The minimum immutable data the email worker needs after claiming an outbox row. */
public record EmailNotificationDelivery(
        String recipientEmail,
        NotificationType notificationType,
        String payloadJson
) {
}
