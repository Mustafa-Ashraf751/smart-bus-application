package com.verysmartbus.controller;

import com.verysmartbus.dto.request.TripStationRequestDto;
import com.verysmartbus.dto.request.TripStationArrivalTimeUpdateRequestDto;
import com.verysmartbus.dto.response.TripStationResponseDto;
import com.verysmartbus.security.AuthenticatedUserResolver;
import com.verysmartbus.service.TripStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bus-admin/trips/{tripId}/stations")
@RequiredArgsConstructor
public class TripStationController {

    private final TripStationService tripStationService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping
    public ResponseEntity<TripStationResponseDto> addStation(
            @PathVariable Long tripId,
            @Valid @RequestBody TripStationRequestDto dto,
            Authentication authentication
    ) {
        TripStationResponseDto created = tripStationService.addStation(
                userResolver.getCurrentUserId(authentication), tripId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TripStationResponseDto> getAllByTripId(@PathVariable Long tripId, Authentication authentication) {
        return tripStationService.findAllByTripId(userResolver.getCurrentUserId(authentication), tripId);
    }

    @PatchMapping("/{tripStationId}/arrival-time")
    public TripStationResponseDto updateArrivalTime(
            @PathVariable Long tripId,
            @PathVariable Long tripStationId,
            @Valid @RequestBody TripStationArrivalTimeUpdateRequestDto dto,
            Authentication authentication
    ) {
        return tripStationService.updateArrivalTime(
                userResolver.getCurrentUserId(authentication), tripId, tripStationId, dto);
    }

    @DeleteMapping("/{tripStationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long tripId,
            @PathVariable Long tripStationId,
            Authentication authentication
    ) {
        tripStationService.delete(userResolver.getCurrentUserId(authentication), tripId, tripStationId);
        return ResponseEntity.noContent().build();
    }
}
