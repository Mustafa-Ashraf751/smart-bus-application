package com.verysmartbus.dto.response;

public record StationPresenceResponseDto(
        Long stationId,
        String stationName,
        int consecutiveInsideCount,
        boolean arrivalConfirmed
) {
}
