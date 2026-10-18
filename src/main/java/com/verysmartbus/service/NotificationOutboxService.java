package com.verysmartbus.service;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.email.EmailNotificationDelivery;
import com.verysmartbus.entity.Notification;
import com.verysmartbus.entity.NotificationOutbox;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import com.verysmartbus.entity.enums.NotificationOutboxStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.NotificationMapper;
import com.verysmartbus.repository.NotificationOutboxRepository;
import com.verysmartbus.websocket.NotificationDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public List<Long> claimPendingBatch(NotificationDeliveryChannel deliveryChannel, int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return notificationOutboxRepository.lockPendingBatch(deliveryChannel.name(), batchSize).stream()
                .peek(outbox -> outbox.claim(now))
                .map(NotificationOutbox::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDelivery getWebSocketDelivery(Long outboxId) {
        NotificationOutbox outbox = findOutbox(outboxId);
        Notification notification = outbox.getNotification();
        NotificationResponseDto response = notificationMapper.toResponseDto(notification);
        return new NotificationDelivery(notification.getRecipient().getEmail(), response);
    }

    @Transactional(readOnly = true)
    public EmailNotificationDelivery getEmailDelivery(Long outboxId) {
        NotificationOutbox outbox = findOutbox(outboxId);
        if (outbox.getDeliveryChannel() != NotificationDeliveryChannel.EMAIL) {
            throw new IllegalStateException("Outbox event is not an email delivery.");
        }
        return new EmailNotificationDelivery(
                outbox.getRecipientEmail(),
                outbox.getNotification().getType(),
                outbox.getNotification().getPayloadJson()
        );
    }

    @Transactional
    public void markSent(Long outboxId) {
        findOutbox(outboxId).markSent(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void scheduleRetry(Long outboxId, String error, long retryDelaySeconds) {
        String safeError = error == null ? "Notification delivery failed." : error;
        findOutbox(outboxId).scheduleRetry(
                OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(retryDelaySeconds),
                safeError.substring(0, Math.min(safeError.length(), 1000))
        );
    }

    @Transactional
    public void scheduleEmailRetry(
            Long outboxId,
            String error,
            long initialRetryDelaySeconds,
            long maxRetryDelaySeconds,
            int maxAttempts
    ) {
        NotificationOutbox outbox = findOutbox(outboxId);
        int failureCount = outbox.getAttemptCount() + 1;
        if (failureCount >= maxAttempts) {
            outbox.markFailed(safeError(error));
            return;
        }

        long exponent = Math.max(0, failureCount - 1);
        long multiplier = 1L << Math.min(exponent, 30);
        long delay = Math.min(maxRetryDelaySeconds, Math.max(1, initialRetryDelaySeconds) * multiplier);
        outbox.scheduleRetry(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delay), safeError(error));
    }

    @Transactional
    public void markFailed(Long outboxId, String error) {
        findOutbox(outboxId).markFailed(safeError(error));
    }

    @Transactional
    public void recoverStaleProcessing(
            NotificationDeliveryChannel deliveryChannel,
            long processingTimeoutSeconds,
            long retryDelaySeconds
    ) {
        OffsetDateTime staleBefore = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(processingTimeoutSeconds);
        notificationOutboxRepository
                .findAllByDeliveryChannelAndStatusAndLockedAtBefore(
                        deliveryChannel, NotificationOutboxStatus.PROCESSING, staleBefore)
                .forEach(outbox -> outbox.scheduleRetry(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(retryDelaySeconds),
                        "Recovered after an interrupted delivery attempt."));
    }

    @Transactional
    public void recoverStaleEmailProcessing(
            long processingTimeoutSeconds,
            long initialRetryDelaySeconds,
            long maxRetryDelaySeconds,
            int maxAttempts
    ) {
        OffsetDateTime staleBefore = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(processingTimeoutSeconds);
        notificationOutboxRepository
                .findAllByDeliveryChannelAndStatusAndLockedAtBefore(
                        NotificationDeliveryChannel.EMAIL, NotificationOutboxStatus.PROCESSING, staleBefore)
                .forEach(outbox -> scheduleEmailRetry(
                        outbox.getId(),
                        "Recovered after an interrupted email delivery attempt.",
                        initialRetryDelaySeconds,
                        maxRetryDelaySeconds,
                        maxAttempts));
    }

    private String safeError(String error) {
        return error == null || error.isBlank()
                ? "Notification delivery failed."
                : error.substring(0, Math.min(error.length(), 1000));
    }

    private NotificationOutbox findOutbox(Long outboxId) {
        return notificationOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification outbox event", outboxId));
    }
}
