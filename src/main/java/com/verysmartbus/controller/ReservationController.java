package com.verysmartbus.controller;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Who may do what is decided in ReservationService (owner / assigned driver / assigned
 * bus admin / ADMIN), not here, so the rules live next to the data they depend on.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> create(
            @Valid @RequestBody ReservationRequestDto request,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.create(currentUserId, request));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponseDto> cancel(
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(reservationService.cancel(currentUserId, reservationId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponseDto>> getMine(Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(reservationService.getForUser(currentUserId));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ReservationResponseDto>> getPassengers(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(reservationService.getPassengers(currentUserId, tripId));
    }

    @GetMapping("/trip/{tripId}/bus")
    public ResponseEntity<BusResponseDto> getBus(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(reservationService.getBus(currentUserId, tripId));
    }
}
