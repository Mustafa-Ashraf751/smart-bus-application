package com.verysmartbus.controller;

import com.verysmartbus.dto.request.StationRequestDto;
import com.verysmartbus.dto.response.StationResponseDto;
import com.verysmartbus.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @PostMapping
    public ResponseEntity<StationResponseDto> create(@Valid @RequestBody StationRequestDto dto) {
        StationResponseDto created = stationService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<StationResponseDto> getAll() {
        return stationService.findAll();
    }

    @GetMapping("/{id}")
    public StationResponseDto getById(@PathVariable Long id) {
        return stationService.findById(id);
    }

    @PutMapping("/{id}")
    public StationResponseDto update(@PathVariable Long id, @Valid @RequestBody StationRequestDto dto) {
        return stationService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
