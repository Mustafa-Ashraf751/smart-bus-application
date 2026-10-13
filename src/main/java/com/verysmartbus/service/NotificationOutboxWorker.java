package com.verysmartbus.service;

import com.verysmartbus.websocket.NotificationWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.notifications.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxService notificationOutboxService;
    private final NotificationWebSocketPublisher notificationWebSocketPublisher;

    @Value("${app.notifications.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.notifications.outbox.retry-delay-seconds:10}")
    private long retryDelaySeconds;

    @Value("${app.notifications.outbox.processing-timeout-seconds:60}")
    private long processingTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.notifications.outbox.poll-delay-ms:5000}")
    public void dispatchPendingNotifications() {
        notificationOutboxService.recoverStaleProcessing(processingTimeoutSeconds);
        for (Long outboxId : notificationOutboxService.claimPendingBatch(batchSize)) {
            try {
                notificationWebSocketPublisher.publish(notificationOutboxService.getDelivery(outboxId));
                notificationOutboxService.markSent(outboxId);
            } catch (RuntimeException exception) {
                notificationOutboxService.scheduleRetry(outboxId, exception.getMessage(), retryDelaySeconds);
            }
        }
    }
}
