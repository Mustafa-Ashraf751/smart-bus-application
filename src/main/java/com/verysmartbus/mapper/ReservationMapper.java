package com.verysmartbus.mapper;

import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public Reservation toEntity(AppUser user,
                                Trip trip,
                                TripStation pickup,
                                TripStation dropoff,
                                ReservationStatus status,
                                ReservationSource source) {
        return Reservation.builder()
                .user(user)
                .trip(trip)
                .pickupTripStation(pickup)
                .dropoffTripStation(dropoff)
                .status(status)
                .source(source)
                .build();
    }

    public ReservationResponseDto toResponseDto(Reservation reservation) {
        TripStation pickup = reservation.getPickupTripStation();
        TripStation dropoff = reservation.getDropoffTripStation();

        // dropoff is null only for legacy reservations created before drop-off existed.
        return new ReservationResponseDto(
                reservation.getReservationId(),
                reservation.getUser().getId(),
                reservation.getTrip().getId(),
                pickup.getId(),
                pickup.getStation().getName(),
                dropoff == null ? null : dropoff.getId(),
                dropoff == null ? null : dropoff.getStation().getName(),
                reservation.getStatus(),
                reservation.getSource(),
                reservation.getReservedAt(),
                reservation.getCancelledAt()
        );
    }
}
