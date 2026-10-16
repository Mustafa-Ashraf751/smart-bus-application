package com.verysmartbus.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.verysmartbus.entity.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
            @JsonFormat(pattern = "HH:mm:ss")
            @Schema(type = "string", format = "time", example = "13:34:44")
            LocalTime expectedArrivalTime
    ) {
    }
}
