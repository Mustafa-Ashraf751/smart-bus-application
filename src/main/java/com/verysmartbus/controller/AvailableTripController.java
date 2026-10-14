package com.verysmartbus.controller;

import com.verysmartbus.dto.response.AvailableTripResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.AvailableTripService;
import com.verysmartbus.service.DailyTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class AvailableTripController {

    private final DailyTripService dailyTripService;
    private final AvailableTripService availableTripService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * Trips the logged-in user can book. Optional ?routeId= narrows it to one route.
     * Opening this screen is an app open too, so today's trips are generated first if needed.
     */
    @GetMapping("/available")
    public ResponseEntity<List<AvailableTripResponseDto>> available(
            @RequestParam(required = false) Long routeId,
            Authentication authentication) {
        Long currentUserId = userResolver.getCurrentUserId(authentication);
        dailyTripService.ensureTodayTrips();
        return ResponseEntity.ok(availableTripService.findAvailable(currentUserId, routeId));
    }
}
