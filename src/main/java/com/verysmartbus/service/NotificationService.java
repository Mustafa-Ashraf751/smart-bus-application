package com.verysmartbus.service;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Notification;
import com.verysmartbus.entity.NotificationOutbox;
import com.verysmartbus.entity.enums.NotificationType;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.NotificationMapper;
import com.verysmartbus.repository.NotificationOutboxRepository;
import com.verysmartbus.repository.NotificationRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Future business services call this inside their own transaction. The durable inbox record and
     * the outbox event therefore commit together with the business change.
     */
    @Transactional
    public NotificationResponseDto createForUser(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String payloadJson,
            String eventKey
    ) {
        return notificationOutboxRepository.findByEventKey(eventKey)
                .map(existing -> notificationMapper.toResponseDto(existing.getNotification()))
                .orElseGet(() -> createNotificationAndOutbox(
                        recipientUserId, type, title, message, payloadJson, eventKey));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getMine(Long currentUserId) {
        return notificationRepository.findAllByRecipient_IdOrderByCreatedAtDesc(currentUserId).stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long currentUserId) {
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(currentUserId);
    }

    @Transactional
    public NotificationResponseDto markRead(Long currentUserId, Long notificationId) {
        Notification notification = findNotification(notificationId);
        assertOwnership(notification, currentUserId);
        notification.markRead(OffsetDateTime.now(ZoneOffset.UTC));
        return notificationMapper.toResponseDto(notification);
    }

    @Transactional
    public void markAllRead(Long currentUserId) {
        OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
        notificationRepository.findAllByRecipient_IdOrderByCreatedAtDesc(currentUserId)
                .forEach(notification -> notification.markRead(readAt));
    }

    private NotificationResponseDto createNotificationAndOutbox(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String payloadJson,
            String eventKey
    ) {
        AppUser recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recipientUserId));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .payloadJson(payloadJson)
                .build());
        notificationOutboxRepository.save(NotificationOutbox.builder()
                .notification(notification)
                .eventKey(eventKey)
                .nextAttemptAt(now)
                .build());
        return notificationMapper.toResponseDto(notification);
    }

    private Notification findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    }

    private void assertOwnership(Notification notification, Long currentUserId) {
        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You do not own this notification.");
        }
    }
}
