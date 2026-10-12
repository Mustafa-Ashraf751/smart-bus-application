package com.verysmartbus.service;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.entity.Notification;
import com.verysmartbus.entity.NotificationOutbox;
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
    public List<Long> claimPendingBatch(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return notificationOutboxRepository.lockPendingBatch(batchSize).stream()
                .peek(outbox -> outbox.claim(now))
                .map(NotificationOutbox::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDelivery getDelivery(Long outboxId) {
        NotificationOutbox outbox = findOutbox(outboxId);
        Notification notification = outbox.getNotification();
        NotificationResponseDto response = notificationMapper.toResponseDto(notification);
        return new NotificationDelivery(notification.getRecipient().getEmail(), response);
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
    public void recoverStaleProcessing(long processingTimeoutSeconds) {
        OffsetDateTime staleBefore = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(processingTimeoutSeconds);
        notificationOutboxRepository
                .findAllByStatusAndLockedAtBefore(NotificationOutboxStatus.PROCESSING, staleBefore)
                .forEach(outbox -> outbox.scheduleRetry(OffsetDateTime.now(ZoneOffset.UTC),
                        "Recovered after an interrupted delivery attempt."));
    }

    private NotificationOutbox findOutbox(Long outboxId) {
        return notificationOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification outbox event", outboxId));
    }
}
