package com.verysmartbus.controller;

import com.verysmartbus.dto.response.NotificationResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping("/me")
    public List<NotificationResponseDto> getMine(Authentication authentication) {
        return notificationService.getMine(userResolver.getCurrentUserId(authentication));
    }

    @GetMapping("/me/unread-count")
    public Map<String, Long> getUnreadCount(Authentication authentication) {
        long unreadCount = notificationService.getUnreadCount(userResolver.getCurrentUserId(authentication));
        return Map.of("unreadCount", unreadCount);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponseDto markRead(@PathVariable Long notificationId, Authentication authentication) {
        return notificationService.markRead(userResolver.getCurrentUserId(authentication), notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(userResolver.getCurrentUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
