package com.verysmartbus.controller;

import com.verysmartbus.dto.request.RouteRequestDto;
import com.verysmartbus.dto.request.RoutePathRequestDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<RouteResponseDto> create(@Valid @RequestBody RouteRequestDto dto) {
        RouteResponseDto created = routeService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RouteResponseDto> getAll() {
        return routeService.findAll();
    }

    @GetMapping("/{id}")
    public RouteResponseDto getById(@PathVariable Long id) {
        return routeService.findById(id);
    }

    @PutMapping("/{id}")
    public RouteResponseDto update(@PathVariable Long id, @Valid @RequestBody RouteRequestDto dto) {
        return routeService.update(id, dto);
    }

    @PutMapping("/{id}/path")
    public RouteResponseDto updatePath(
            @PathVariable Long id,
            @Valid @RequestBody RoutePathRequestDto dto
    ) {
        return routeService.updatePath(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
