package com.verysmartbus.controller;

import com.verysmartbus.dto.request.BusAdminTripBusUpdateRequestDto;
import com.verysmartbus.dto.request.BusAdminTripDriverUpdateRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.BusAdminTripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bus-admin/trips")
@RequiredArgsConstructor
public class BusAdminTripController {

    private final BusAdminTripService busAdminTripService;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping("/me")
    public List<TripResponseDto> getMyTrips(Authentication authentication) {
        return busAdminTripService.findMyTrips(userResolver.getCurrentUserId(authentication));
    }

    @GetMapping("/{tripId}")
    public TripResponseDto getMyTrip(@PathVariable Long tripId, Authentication authentication) {
        return busAdminTripService.findMyTrip(userResolver.getCurrentUserId(authentication), tripId);
    }

    @PatchMapping("/{tripId}/bus")
    public TripResponseDto updateBus(
            @PathVariable Long tripId,
            @Valid @RequestBody BusAdminTripBusUpdateRequestDto request,
            Authentication authentication
    ) {
        return busAdminTripService.updateBus(userResolver.getCurrentUserId(authentication), tripId, request);
    }

    @PatchMapping("/{tripId}/driver")
    public TripResponseDto updateDriver(
            @PathVariable Long tripId,
            @Valid @RequestBody BusAdminTripDriverUpdateRequestDto request,
            Authentication authentication
    ) {
        return busAdminTripService.updateDriver(userResolver.getCurrentUserId(authentication), tripId, request);
    }

    @PatchMapping("/{tripId}/start")
    public TripResponseDto start(@PathVariable Long tripId, Authentication authentication) {
        return busAdminTripService.start(userResolver.getCurrentUserId(authentication), tripId);
    }

    @PatchMapping("/{tripId}/complete")
    public TripResponseDto complete(@PathVariable Long tripId, Authentication authentication) {
        return busAdminTripService.complete(userResolver.getCurrentUserId(authentication), tripId);
    }
}
