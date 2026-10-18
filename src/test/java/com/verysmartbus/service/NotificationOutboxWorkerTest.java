package com.verysmartbus.service;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.websocket.NotificationDelivery;
import com.verysmartbus.websocket.NotificationWebSocketPublisher;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxWorkerTest {

    private final NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
    private final NotificationWebSocketPublisher publisher = mock(NotificationWebSocketPublisher.class);
    private final NotificationOutboxWorker worker = new NotificationOutboxWorker(outboxService, publisher);

    @Test
    void claimsThenPublishesAndMarksEveryPendingNotificationAsSent() {
        NotificationDelivery first = mock(NotificationDelivery.class);
        NotificationDelivery second = mock(NotificationDelivery.class);
        ReflectionTestUtils.setField(worker, "batchSize", 50);
        ReflectionTestUtils.setField(worker, "retryDelaySeconds", 10L);
        ReflectionTestUtils.setField(worker, "processingTimeoutSeconds", 60L);
        when(outboxService.claimPendingBatch(NotificationDeliveryChannel.WEBSOCKET, 50)).thenReturn(List.of(11L, 12L));
        when(outboxService.getWebSocketDelivery(11L)).thenReturn(first);
        when(outboxService.getWebSocketDelivery(12L)).thenReturn(second);

        worker.dispatchPendingNotifications();

        verify(outboxService).recoverStaleProcessing(NotificationDeliveryChannel.WEBSOCKET, 60L, 10L);
        verify(publisher).publish(first);
        verify(publisher).publish(second);
        verify(outboxService).markSent(11L);
        verify(outboxService).markSent(12L);
    }

    @Test
    void schedulesRetryInsteadOfLosingANotificationWhenWebSocketDeliveryFails() {
        NotificationDelivery delivery = mock(NotificationDelivery.class);
        ReflectionTestUtils.setField(worker, "batchSize", 50);
        ReflectionTestUtils.setField(worker, "retryDelaySeconds", 10L);
        ReflectionTestUtils.setField(worker, "processingTimeoutSeconds", 60L);
        when(outboxService.claimPendingBatch(NotificationDeliveryChannel.WEBSOCKET, 50)).thenReturn(List.of(11L));
        when(outboxService.getWebSocketDelivery(11L)).thenReturn(delivery);
        doThrow(new IllegalStateException("socket unavailable")).when(publisher).publish(delivery);

        worker.dispatchPendingNotifications();

        verify(outboxService).scheduleRetry(11L, "socket unavailable", 10L);
    }
}
