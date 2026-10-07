package com.verysmartbus.controller;

import com.verysmartbus.dto.request.BusRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.entity.Bus.BusStatus;
import com.verysmartbus.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    @PostMapping
    public ResponseEntity<BusResponseDto> create(@Valid @RequestBody BusRequestDto dto) {
        BusResponseDto created = busService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<BusResponseDto> getAll(@RequestParam(required = false) BusStatus status) {
        return busService.getAll(status);
    }

    @GetMapping("/{id}")
    public BusResponseDto getById(@PathVariable Long id) {
        return busService.getById(id);
    }

    @PutMapping("/{id}")
    public BusResponseDto update(@PathVariable Long id, @Valid @RequestBody BusRequestDto dto) {
        return busService.update(id, dto);
    }

    @PatchMapping("/{id}/activate")
    public BusResponseDto activate(@PathVariable Long id) {
        return busService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public BusResponseDto deactivate(@PathVariable Long id) {
        return busService.deactivate(id);
    }
}
