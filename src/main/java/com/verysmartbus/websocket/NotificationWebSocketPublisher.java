package com.verysmartbus.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketPublisher {

    private static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(NotificationDelivery delivery) {
        messagingTemplate.convertAndSendToUser(
                delivery.recipientUsername(),
                NOTIFICATION_QUEUE,
                delivery.notification()
        );
    }
}
