package com.verysmartbus.controller;

import com.verysmartbus.dto.request.LocationUpdateRequestDto;
import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.security.TripLocationAccessAuthorizer;
import com.verysmartbus.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shared live-trip operations. The service verifies the current user's actual relationship to
 * each trip, rather than trusting a frontend role choice.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripOperationController {

    private final TripService tripService;
    private final AuthenticatedUserResolver userResolver;
    private final TripLocationAccessAuthorizer locationAccessAuthorizer;

    @GetMapping("/me")
    public List<TripResponseDto> getMyTrips(Authentication authentication) {
        return tripService.findMyTrips(userResolver.getCurrentUserId(authentication));
    }

    /** Current location is visible only to a participant or authorized operational user. */
    @GetMapping("/{tripId}/location")
    public TripLocationResponseDto getCurrentLocation(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        locationAccessAuthorizer.authorize(authentication, tripId);
        return tripService.findCurrentLocation(tripId);
    }

    @PatchMapping("/{tripId}/start")
    public ResponseEntity<TripResponseDto> startTrip(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tripService.startTrip(userResolver.getCurrentUserId(authentication), tripId));
    }

    @PatchMapping("/{tripId}/location")
    public TripLocationResponseDto updateCurrentLocation(
            @PathVariable Long tripId,
            @Valid @RequestBody LocationUpdateRequestDto dto,
            Authentication authentication
    ) {
        return tripService.updateCurrentLocation(tripId, userResolver.getCurrentUserId(authentication), dto);
    }

    @PatchMapping("/{tripId}/complete")
    public ResponseEntity<TripResponseDto> completeTrip(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tripService.completeTrip(userResolver.getCurrentUserId(authentication), tripId));
    }
}
