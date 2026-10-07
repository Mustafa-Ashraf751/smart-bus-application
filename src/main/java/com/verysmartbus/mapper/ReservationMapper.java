package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import org.springframework.stereotype.Component;


@Component
public class ReservationMapper {

    public Reservation toEntity(ReservationRequestDto dto, AppUser user, Trip trip, TripStation pickupTripStation) {

        return Reservation.builder()
                .user(user)
                .trip(trip)
                .pickupTripStation(pickupTripStation)
                .build();
    }

    public ReservationResponseDto toResponseDto(Reservation entity) {
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
