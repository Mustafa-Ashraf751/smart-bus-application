package com.verysmartbus.controller;

import com.verysmartbus.dto.request.DriverLocationUpdateRequestDto;
import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver/trips")
@RequiredArgsConstructor
public class DriverTripLocationController {

    private final TripService tripService;
    private final AuthenticatedUserResolver userResolver;

    @PatchMapping("/{tripId}/location")
    public TripLocationResponseDto updateCurrentLocation(
            @PathVariable Long tripId,
            @Valid @RequestBody DriverLocationUpdateRequestDto dto,
            Authentication authentication
    ) {
        Long authenticatedDriverId = userResolver.getCurrentUserId(authentication);
        return tripService.updateCurrentLocation(tripId, authenticatedDriverId, dto);
    }
}
