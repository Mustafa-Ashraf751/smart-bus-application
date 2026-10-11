package com.verysmartbus.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    public static final String APPLICATION_DESTINATION_PREFIX = "/app";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TRIP_LOCATION_TOPIC_PREFIX = "/topic/trips/";
    private static final String TRIP_LOCATION_TOPIC_SUFFIX = "/location";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TripLocationAccessAuthorizer accessAuthorizer;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("A bearer token is required to connect to WebSocket.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!jwtService.isTokenValid(token)) {
            throw new AccessDeniedException("WebSocket token is invalid or expired.");
        }

        try {
            Long userId = jwtService.extractUserId(token);
            UserPrincipal principal = userDetailsService.loadUserById(userId);
            if (!principal.isEnabled()) {
                throw new AccessDeniedException("This user account is disabled.");
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            accessor.setUser(authentication);
        } catch (UsernameNotFoundException exception) {
            throw new AccessDeniedException("WebSocket user no longer exists.");
        }
    }

    /**
     * Only the server publishes to /topic (trip locations). Without this check a client could SEND
     * straight to /topic/trips/{id}/location and the broker would relay a fake location to every subscriber.
     */
    private void authorizeSend(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication)) {
            throw new AccessDeniedException("WebSocket messages require an authenticated connection.");
        }
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(APPLICATION_DESTINATION_PREFIX + "/")) {
            throw new AccessDeniedException("Clients can send only to " + APPLICATION_DESTINATION_PREFIX + " destinations.");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null
                || !destination.startsWith(TRIP_LOCATION_TOPIC_PREFIX)
                || !destination.endsWith(TRIP_LOCATION_TOPIC_SUFFIX)) {
            throw new AccessDeniedException("Subscription destination is not allowed.");
        }

        String tripIdText = destination
                .substring(TRIP_LOCATION_TOPIC_PREFIX.length(), destination.length() - TRIP_LOCATION_TOPIC_SUFFIX.length());
        try {
            Long tripId = Long.valueOf(tripIdText);
            accessAuthorizer.authorize((Authentication) accessor.getUser(), tripId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Trip location topic must contain a numeric trip ID.");
        }
    }
}
