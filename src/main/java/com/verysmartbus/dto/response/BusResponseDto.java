package com.verysmartbus.dto.response;

import com.verysmartbus.entity.Bus.BusStatus;

import java.time.Instant;

public record BusResponseDto(
        Long id,
        String busNumber,
        Integer capacity,
        BusStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
