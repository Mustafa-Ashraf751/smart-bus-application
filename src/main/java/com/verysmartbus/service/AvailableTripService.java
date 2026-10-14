package com.verysmartbus.service;

import com.verysmartbus.dto.response.AvailableTripResponseDto;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.TripStationStatus;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lists the trips a passenger can book: SCHEDULED, today or later, on an ACTIVE route.
 * That is exactly what ReservationService.create accepts, so nothing listed here is refused
 * for its date or status. Trips made by an admin and trips generated automatically look the same.
 */
@Service
@RequiredArgsConstructor
public class AvailableTripService {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final TripRepository tripRepository;
    private final TripStationRepository tripStationRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<AvailableTripResponseDto> findAvailable(Long currentUserId, Long routeId) {
        List<Trip> trips = tripRepository.findBookableTrips(LocalDate.now(CAIRO)).stream()
                .filter(trip -> routeId == null || trip.getRoute().getId().equals(routeId))
                .toList();
        if (trips.isEmpty()) {
            return List.of();
        }

        // Three queries for the whole list, instead of three per trip.
        List<Long> tripIds = trips.stream().map(Trip::getId).toList();

        Map<Long, List<TripStation>> stopsByTrip = tripStationRepository
                .findByTrip_IdInAndStatus(tripIds, TripStationStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(stop -> stop.getTrip().getId()));

        Map<Long, List<Reservation>> liveByTrip = reservationRepository
                .findByTrip_IdInAndStatusNot(tripIds, ReservationStatus.CANCELLED).stream()
                .collect(Collectors.groupingBy(reservation -> reservation.getTrip().getId()));

        return trips.stream()
                .map(trip -> toDto(trip,
                        stopsByTrip.getOrDefault(trip.getId(), List.of()),
                        liveByTrip.getOrDefault(trip.getId(), List.of()),
                        currentUserId))
                .toList();
    }

    private AvailableTripResponseDto toDto(Trip trip, List<TripStation> stops,
                                           List<Reservation> live, Long currentUserId) {
        long seatsTaken = countWithStatus(live, ReservationStatus.RESERVED);
        long waitlisted = countWithStatus(live, ReservationStatus.WAITLISTED);
        int capacity = trip.getBus().getCapacity();
        long availableSeats = Math.max(0, capacity - seatsTaken);

        Reservation mine = live.stream()
                .filter(reservation -> reservation.getUser().getId().equals(currentUserId))
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED
                        || reservation.getStatus() == ReservationStatus.WAITLISTED)
                .findFirst()
                .orElse(null);

        List<AvailableTripResponseDto.Stop> stopDtos = stops.stream()
                .sorted(Comparator.comparing(TripStation::getStopOrder))
                .map(stop -> new AvailableTripResponseDto.Stop(
                        stop.getId(),
                        stop.getStation().getName(),
                        stop.getStopOrder(),
                        stop.getExpectedArrivalTime()))
                .toList();

        return new AvailableTripResponseDto(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getName(),
                trip.getServiceDate(),
                trip.getScheduledStartTime(),
                capacity,
                seatsTaken,
                availableSeats,
                waitlisted,
                availableSeats == 0,
                mine == null ? null : mine.getReservationId(),
                mine == null ? null : mine.getStatus(),
                stopDtos);
    }

    private long countWithStatus(List<Reservation> reservations, ReservationStatus status) {
        return reservations.stream().filter(reservation -> reservation.getStatus() == status).count();
    }
}
