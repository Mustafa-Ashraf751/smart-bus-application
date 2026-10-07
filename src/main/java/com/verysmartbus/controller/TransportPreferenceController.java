package com.verysmartbus.controller;


import com.verysmartbus.dto.request.TransportPreferenceRequestDto;
import com.verysmartbus.dto.response.TransportPreferenceResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.TransportPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport-preferences")
@RequiredArgsConstructor
public class TransportPreferenceController {

    private final TransportPreferenceService transportPreferenceService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<TransportPreferenceResponseDto> create(
            @Valid @RequestBody TransportPreferenceRequestDto dto,
            Authentication authentication) {

        Long currentUserId = userResolver.getCurrentUserId(authentication);
        TransportPreferenceResponseDto created = transportPreferenceService.create(currentUserId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{preferenceId}")
    public ResponseEntity<TransportPreferenceResponseDto> update(
            @PathVariable Long preferenceId,
            @Valid @RequestBody TransportPreferenceRequestDto dto,
            Authentication authentication) {

        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(transportPreferenceService.update(currentUserId, preferenceId, dto));
    }


    @GetMapping("/me")
    public ResponseEntity<List<TransportPreferenceResponseDto>> getMine(Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(transportPreferenceService.getForUser(currentUserId));
    }

    @DeleteMapping("/{preferenceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long preferenceId,
            Authentication authentication) {

        Long currentUserId = userResolver.getCurrentUserId(authentication);
        transportPreferenceService.delete(currentUserId, preferenceId);
        return ResponseEntity.noContent().build();
    }

    // ضيف الميثود دي في TransportPreferenceController.java
    @PatchMapping("/{preferenceId}/toggle-active")
    public ResponseEntity<TransportPreferenceResponseDto> toggleActive(
            @PathVariable Long preferenceId,
            Authentication authentication) {

        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(transportPreferenceService.toggleActive(currentUserId, preferenceId));
    }
}
