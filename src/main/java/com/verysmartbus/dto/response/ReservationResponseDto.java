package com.verysmartbus.dto.response;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.enums.ReservationStatus;

import java.time.LocalDateTime;


public record ReservationResponseDto(
        Long reservationId,
        Long userId,
        Long tripId,
        Long pickupTripStationId,
        String pickupStationName,
        ReservationStatus status,
        LocalDateTime reservedAt,
        LocalDateTime cancelledAt
) {
    public static ReservationResponseDto fromEntity(Reservation entity) {
        return new ReservationResponseDto(
                entity.getReservationId(),
                entity.getUser().getId(),
                entity.getTrip().getId(),
                entity.getPickupTripStation().getId(),
                entity.getPickupTripStation().getStation().getName(),
                entity.getStatus(),
                entity.getReservedAt(),
                entity.getCancelledAt()
        );
    }
}
