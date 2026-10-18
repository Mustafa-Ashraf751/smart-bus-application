package com.verysmartbus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.email.EmailNotificationProperties;
import com.verysmartbus.email.RegistrationApprovedEmailPayload;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Notification;
import com.verysmartbus.entity.NotificationOutbox;
import com.verysmartbus.entity.enums.NotificationType;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
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
    private final ObjectMapper objectMapper;
    private final EmailNotificationProperties emailProperties;

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
        return notificationOutboxRepository.findByEventKeyAndDeliveryChannel(
                        eventKey, NotificationDeliveryChannel.WEBSOCKET)
                .map(existing -> notificationMapper.toResponseDto(existing.getNotification()))
                .orElseGet(() -> createNotificationAndOutbox(
                        recipientUserId, type, title, message, payloadJson, eventKey));
    }

    /**
     * Approval is both an inbox notification and an email event. Both outbox rows are saved in
     * the caller's transaction, so an ACTIVE account cannot commit without its durable email job.
     */
    @Transactional
    public void createRegistrationApprovedNotification(AppUser user) {
        String eventKey = "registration-approved:" + user.getId();
        if (notificationOutboxRepository.findByEventKeyAndDeliveryChannel(
                eventKey, NotificationDeliveryChannel.EMAIL).isPresent()) {
            return;
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(new RegistrationApprovedEmailPayload(
                    user.getName(), user.getUsername(), user.getEmail(), emailProperties.getFrontendLoginUrl()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not create the registration approval email event.", exception);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(user)
                .type(NotificationType.REGISTRATION_APPROVED)
                .title("Your account has been approved")
                .message("Your VerySmartBus account is active. You can now log in.")
                .payloadJson(payloadJson)
                .build());

        notificationOutboxRepository.save(NotificationOutbox.builder()
                .notification(notification)
                .eventKey(eventKey)
                .deliveryChannel(NotificationDeliveryChannel.WEBSOCKET)
                .nextAttemptAt(now)
                .build());
        notificationOutboxRepository.save(NotificationOutbox.builder()
                .notification(notification)
                .eventKey(eventKey)
                .deliveryChannel(NotificationDeliveryChannel.EMAIL)
                .recipientEmail(user.getEmail())
                .nextAttemptAt(now)
                .build());
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
                .deliveryChannel(NotificationDeliveryChannel.WEBSOCKET)
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
