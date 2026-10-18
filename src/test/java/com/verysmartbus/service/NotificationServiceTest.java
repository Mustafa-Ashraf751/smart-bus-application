package com.verysmartbus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verysmartbus.email.EmailNotificationProperties;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Notification;
import com.verysmartbus.entity.NotificationOutbox;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import com.verysmartbus.entity.enums.NotificationType;
import com.verysmartbus.mapper.NotificationMapper;
import com.verysmartbus.repository.NotificationOutboxRepository;
import com.verysmartbus.repository.NotificationRepository;
import com.verysmartbus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
    private final NotificationService notificationService = notificationService();

    @Test
    void approvalCreatesOneInAppAndOneEmailJobWithTheSameBusinessEventKey() {
        AppUser user = AppUser.builder()
                .id(81L)
                .name("Nour Ahmed")
                .username("nour.ahmed")
                .email("nour@example.com")
                .build();
        when(outboxRepository.findByEventKeyAndDeliveryChannel(
                "registration-approved:81", NotificationDeliveryChannel.EMAIL)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createRegistrationApprovedNotification(user);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.REGISTRATION_APPROVED);
        assertThat(notificationCaptor.getValue().getPayloadJson())
                .contains("nour.ahmed", "nour@example.com", "http://localhost:5173/login");

        ArgumentCaptor<NotificationOutbox> outboxCaptor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(outboxRepository, times(2)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getAllValues())
                .extracting(NotificationOutbox::getDeliveryChannel)
                .containsExactlyInAnyOrder(NotificationDeliveryChannel.WEBSOCKET, NotificationDeliveryChannel.EMAIL);
        assertThat(outboxCaptor.getAllValues())
                .filteredOn(outbox -> outbox.getDeliveryChannel() == NotificationDeliveryChannel.EMAIL)
                .singleElement()
                .satisfies(outbox -> assertThat(outbox.getRecipientEmail()).isEqualTo("nour@example.com"));
    }

    private NotificationService notificationService() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setFrontendLoginUrl("http://localhost:5173/login");
        return new NotificationService(
                notificationRepository,
                outboxRepository,
                mock(UserRepository.class),
                mock(NotificationMapper.class),
                new ObjectMapper(),
                properties);
    }
}
