package com.verysmartbus.websocket;

import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.event.TripLocationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TripLocationWebSocketPublisher {

    private static final String TRIP_LOCATION_TOPIC_FORMAT = "/topic/trips/%d/location";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TripLocationUpdatedEvent event) {
        TripLocationResponseDto location = event.location();
        String destination = TRIP_LOCATION_TOPIC_FORMAT.formatted(location.tripId());
        messagingTemplate.convertAndSend(destination, location);
    }
}
