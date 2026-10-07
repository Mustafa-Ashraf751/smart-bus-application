package com.verysmartbus.controller;

import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/trips")
@RequiredArgsConstructor
public class DriverTripController {

    private final TripService tripService;
    private final AuthenticatedUserResolver userResolver;

    @PatchMapping("/{tripId}/start")
    public ResponseEntity<TripResponseDto> startTrip(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long currentDriverId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(tripService.startTrip(currentDriverId, tripId));
    }

    @PatchMapping("/{tripId}/end")
    public ResponseEntity<TripResponseDto> endTrip(
            @PathVariable Long tripId,
            Authentication authentication) {
        Long currentDriverId = userResolver.getCurrentUserId(authentication);
        return ResponseEntity.ok(tripService.endTrip(currentDriverId, tripId));
    }
}