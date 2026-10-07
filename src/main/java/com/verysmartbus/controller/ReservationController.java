package com.verysmartbus.controller;


import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.verysmartbus.dto.response.ReservationResponseDto;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> create(
            @Valid @RequestBody com.verysmartbus.dto.request.ReservationRequestDto dto,
            Authentication authentication) {

        Long currentUserId = userResolver.getCurrentUserId(authentication);
        ReservationResponseDto created = reservationService.create(currentUserId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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

    // TODO: restrict to ADMIN/DRIVER once role-based @PreAuthorize is wired
    // in — this currently lets ANY authenticated user see the full
    // passenger list of any trip.
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ReservationResponseDto>> getForTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(reservationService.getForTrip(tripId));
    }

    @GetMapping("/trip/{tripId}/bus")
    public ResponseEntity<BusResponseDto> getBusForTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(reservationService.getBus(tripId));
    }
}
