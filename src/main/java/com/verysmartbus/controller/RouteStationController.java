package com.verysmartbus.controller;

import com.verysmartbus.dto.request.RouteStationRequestDto;
import com.verysmartbus.dto.response.RouteStationResponseDto;
import com.verysmartbus.service.RouteStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/route-stations")
@RequiredArgsConstructor
public class RouteStationController {

    private final RouteStationService routeStationService;

    @PostMapping
    public ResponseEntity<RouteStationResponseDto> addStation(@Valid @RequestBody RouteStationRequestDto dto) {
        RouteStationResponseDto created = routeStationService.addStation(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RouteStationResponseDto> getAllByRouteId(@RequestParam Long routeId) {
        return routeStationService.findAllByRouteId(routeId);
    }

    @GetMapping("/{id}")
    public RouteStationResponseDto getById(@PathVariable Long id) {
        return routeStationService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeStationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
