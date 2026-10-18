package com.verysmartbus.service;

import com.verysmartbus.email.EmailNotificationDelivery;
import com.verysmartbus.email.EmailNotificationProperties;
import com.verysmartbus.email.EmailNotificationSender;
import com.verysmartbus.email.PermanentEmailDeliveryException;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import com.verysmartbus.entity.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationOutboxWorkerTest {

    private final NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
    private final EmailNotificationSender sender = mock(EmailNotificationSender.class);
    private final EmailNotificationProperties properties = properties();
    private final EmailNotificationOutboxWorker worker = new EmailNotificationOutboxWorker(outboxService, sender, properties);

    @Test
    void claimsOnlyEmailJobsThenSendsAndMarksThemSent() {
        EmailNotificationDelivery delivery = new EmailNotificationDelivery(
                "employee@example.com", NotificationType.REGISTRATION_APPROVED, "{}");
        when(outboxService.claimPendingBatch(NotificationDeliveryChannel.EMAIL, 25)).thenReturn(List.of(41L));
        when(outboxService.getEmailDelivery(41L)).thenReturn(delivery);

        worker.dispatchPendingEmails();

        verify(outboxService).recoverStaleEmailProcessing(120L, 30L, 3600L, 5);
        verify(sender).send(delivery);
        verify(outboxService).markSent(41L);
    }

    @Test
    void temporarySmtpFailureSchedulesABoundedRetry() {
        EmailNotificationDelivery delivery = new EmailNotificationDelivery(
                "employee@example.com", NotificationType.REGISTRATION_APPROVED, "{}");
        when(outboxService.claimPendingBatch(NotificationDeliveryChannel.EMAIL, 25)).thenReturn(List.of(41L));
        when(outboxService.getEmailDelivery(41L)).thenReturn(delivery);
        doThrow(new IllegalStateException("connection unavailable")).when(sender).send(delivery);

        worker.dispatchPendingEmails();

        verify(outboxService).scheduleEmailRetry(
                eq(41L), eq("SMTP delivery failed; it will be retried."), eq(30L), eq(3600L), eq(5));
    }

    @Test
    void permanentFailureStopsAutomaticRetries() {
        EmailNotificationDelivery delivery = new EmailNotificationDelivery(
                "employee@example.com", NotificationType.REGISTRATION_APPROVED, "{}");
        when(outboxService.claimPendingBatch(NotificationDeliveryChannel.EMAIL, 25)).thenReturn(List.of(41L));
        when(outboxService.getEmailDelivery(41L)).thenReturn(delivery);
        doThrow(new PermanentEmailDeliveryException("SMTP authentication failed.")).when(sender).send(any());

        worker.dispatchPendingEmails();

        verify(outboxService).markFailed(41L, "SMTP authentication failed.");
    }

    private static EmailNotificationProperties properties() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setBatchSize(25);
        properties.setProcessingTimeoutSeconds(120);
        properties.setInitialRetryDelaySeconds(30);
        properties.setMaxRetryDelaySeconds(3600);
        properties.setMaxAttempts(5);
        return properties;
    }
}
