package com.verysmartbus.service;

import com.verysmartbus.email.EmailNotificationSender;
import com.verysmartbus.email.EmailNotificationProperties;
import com.verysmartbus.email.PermanentEmailDeliveryException;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Claims only EMAIL jobs. SMTP calls happen after the short database claim transaction ends. */
@Component
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmailNotificationOutboxWorker {

    private final NotificationOutboxService notificationOutboxService;
    private final EmailNotificationSender emailNotificationSender;
    private final EmailNotificationProperties properties;

    @Scheduled(fixedDelayString = "${app.notifications.email.poll-delay-ms:5000}")
    public void dispatchPendingEmails() {
        notificationOutboxService.recoverStaleEmailProcessing(
                properties.getProcessingTimeoutSeconds(),
                properties.getInitialRetryDelaySeconds(),
                properties.getMaxRetryDelaySeconds(),
                properties.getMaxAttempts());

        for (Long outboxId : notificationOutboxService.claimPendingBatch(
                NotificationDeliveryChannel.EMAIL, properties.getBatchSize())) {
            try {
                emailNotificationSender.send(notificationOutboxService.getEmailDelivery(outboxId));
                notificationOutboxService.markSent(outboxId);
            } catch (PermanentEmailDeliveryException exception) {
                notificationOutboxService.markFailed(outboxId, exception.getMessage());
            } catch (RuntimeException exception) {
                // Do not store raw SMTP exceptions: they may contain provider internals or credentials.
                notificationOutboxService.scheduleEmailRetry(
                        outboxId,
                        "SMTP delivery failed; it will be retried.",
                        properties.getInitialRetryDelaySeconds(),
                        properties.getMaxRetryDelaySeconds(),
                        properties.getMaxAttempts());
            }
        }
    }
}
