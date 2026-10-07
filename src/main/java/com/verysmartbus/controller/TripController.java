package com.verysmartbus.controller;

import com.verysmartbus.dto.request.TripRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponseDto> create(@Valid @RequestBody TripRequestDto dto) {
        TripResponseDto created = tripService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TripResponseDto> getAll(@RequestParam(required = false) Long routeId) {
        return routeId == null
                ? tripService.findAll()
                : tripService.findAllByRouteId(routeId);
    }

    @GetMapping("/{id}")
    public TripResponseDto getById(@PathVariable Long id) {
        return tripService.findById(id);
    }

    @PatchMapping("/{id}/start")
    public TripResponseDto start(@PathVariable Long id) {
        return tripService.start(id);
    }

    @PatchMapping("/{id}/complete")
    public TripResponseDto complete(@PathVariable Long id) {
        return tripService.complete(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
