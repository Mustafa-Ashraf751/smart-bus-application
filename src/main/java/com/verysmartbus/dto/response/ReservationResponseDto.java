package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponseDto(
        Long reservationId,
        Long userId,
        Long tripId,
        Long pickupTripStationId,
        String pickupStationName,
        Long dropoffTripStationId,
        String dropoffStationName,
        ReservationStatus status,
        ReservationSource source,
        LocalDateTime reservedAt,
        LocalDateTime cancelledAt
) {
}
