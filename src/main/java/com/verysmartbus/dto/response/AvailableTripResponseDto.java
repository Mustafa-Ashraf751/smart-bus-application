package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;


public record AvailableTripResponseDto(
        Long tripId,
        Long routeId,
        String routeName,
        LocalDate serviceDate,
        OffsetDateTime scheduledStartTime,
        int capacity,
        long seatsTaken,
        long availableSeats,
        long waitlisted,
        boolean full,
        Long myReservationId,
        ReservationStatus myReservationStatus,
        List<Stop> stops
) {

    public record Stop(
            Long tripStationId,
            String stationName,
            Integer stopOrder,
            LocalTime expectedArrivalTime
    ) {
    }
}
