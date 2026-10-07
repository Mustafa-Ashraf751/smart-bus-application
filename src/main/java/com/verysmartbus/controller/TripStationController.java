package com.verysmartbus.controller;

import com.verysmartbus.dto.request.TripStationRequestDto;
import com.verysmartbus.dto.response.TripStationResponseDto;
import com.verysmartbus.service.TripStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/trip-stations")
@RequiredArgsConstructor
public class TripStationController {

    private final TripStationService tripStationService;

    @PostMapping
    public ResponseEntity<TripStationResponseDto> addStation(@Valid @RequestBody TripStationRequestDto dto) {
        TripStationResponseDto created = tripStationService.addStation(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TripStationResponseDto> getAllByTripId(@RequestParam Long tripId) {
        return tripStationService.findAllByTripId(tripId);
    }

    @GetMapping("/{id}")
    public TripStationResponseDto getById(@PathVariable Long id) {
        return tripStationService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripStationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
