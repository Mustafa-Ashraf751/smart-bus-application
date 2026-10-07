package com.verysmartbus.controller;

import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.security.TripLocationAccessAuthorizer;
import com.verysmartbus.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripLocationController {

    private final TripService tripService;
    private final TripLocationAccessAuthorizer accessAuthorizer;

    @GetMapping("/{tripId}/location")
    public TripLocationResponseDto getCurrentLocation(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        accessAuthorizer.authorize(authentication, tripId);
        return tripService.findCurrentLocation(tripId);
    }
}
