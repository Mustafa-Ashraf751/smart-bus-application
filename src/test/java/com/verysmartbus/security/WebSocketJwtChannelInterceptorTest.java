package com.verysmartbus.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class WebSocketJwtChannelInterceptorTest {

    private final WebSocketJwtChannelInterceptor interceptor = new WebSocketJwtChannelInterceptor(
            mock(JwtService.class),
            mock(CustomUserDetailsService.class),
            mock(TripLocationAccessAuthorizer.class)
    );
    private final MessageChannel channel = mock(MessageChannel.class);
    private final Principal user = new UsernamePasswordAuthenticationToken("user", null, List.of());

    @Test
    void rejectsClientMessagesToTheLocationTopic() {
        // Would otherwise be relayed by the broker to every passenger as a fake bus location
        Message<byte[]> spoofedLocation = send("/topic/trips/15/location", user);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(spoofedLocation, channel));
    }

    @Test
    void rejectsMessagesFromUnauthenticatedConnections() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(send("/app/anything", null), channel));
    }

    @Test
    void allowsAuthenticatedMessagesToApplicationDestinations() {
        assertDoesNotThrow(() -> interceptor.preSend(send("/app/anything", user), channel));
    }

    private Message<byte[]> send(String destination, Principal sender) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setUser(sender);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
