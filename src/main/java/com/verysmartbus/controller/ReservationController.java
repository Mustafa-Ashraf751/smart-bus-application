package com.verysmartbus.controller;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.AvailableTripResponseDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.AvailableTripService;
import com.verysmartbus.service.DailyTripService;
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
    private final DailyTripService dailyTripService;
    private final AvailableTripService availableTripService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * Trips the logged-in passenger can book. Optional ?routeId= narrows it to one route.
     * Opening the booking screen also creates today's missing daily trips, when needed.
     */
    @GetMapping("/available-trips")
    public ResponseEntity<List<AvailableTripResponseDto>> getAvailableTrips(
            @RequestParam(required = false) Long routeId,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        dailyTripService.ensureTodayTrips();
        return ResponseEntity.ok(availableTripService.findAvailable(currentUserId, routeId));
    }

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

    /** Opening "my reservations" is an app open too: today's trips and preference reservations appear first. */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponseDto>> getMine(Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        dailyTripService.ensureTodayTrips();
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
