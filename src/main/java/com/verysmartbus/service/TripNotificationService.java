package com.verysmartbus.service;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.NotificationType;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Creates durable passenger notifications for changes to a trip. Delivery is deliberately left to
 * the outbox worker, so a WebSocket failure can never roll back a reservation or trip update.
 */
@Service
@RequiredArgsConstructor
public class TripNotificationService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NotificationService notificationService;
    private final ReservationRepository reservationRepository;

    public void reservationCreated(Reservation reservation) {
        Trip trip = reservation.getTrip();
        NotificationType type = reservation.getStatus() == ReservationStatus.RESERVED
                ? NotificationType.RESERVATION_CONFIRMED
                : NotificationType.RESERVATION_WAITLISTED;
        String title = type == NotificationType.RESERVATION_CONFIRMED
                ? "Reservation confirmed"
                : "Added to waitlist";
        String message = type == NotificationType.RESERVATION_CONFIRMED
                ? "Your reservation for " + tripLabel(trip) + " is confirmed."
                : "" + tripLabel(trip) + " is full. You have been added to the waitlist.";

        notificationService.createForUser(
                reservation.getUser().getId(),
                type,
                title,
                message,
                reservationPayload(reservation),
                "reservation:" + reservation.getReservationId() + ":created"
        );
    }

    public void reservationCancelled(Reservation reservation) {
        notificationService.createForUser(
                reservation.getUser().getId(),
                NotificationType.RESERVATION_CANCELLED,
                "Reservation cancelled",
                "Your reservation for " + tripLabel(reservation.getTrip()) + " has been cancelled.",
                reservationPayload(reservation),
                "reservation:" + reservation.getReservationId() + ":cancelled"
        );
    }

    public void reservationPromoted(Reservation reservation) {
        notificationService.createForUser(
                reservation.getUser().getId(),
                NotificationType.WAITLIST_PROMOTED,
                "Seat confirmed",
                "A seat is now available. Your reservation for " + tripLabel(reservation.getTrip())
                        + " is confirmed.",
                reservationPayload(reservation),
                "reservation:" + reservation.getReservationId() + ":promoted"
        );
    }

    public void tripStarted(Trip trip) {
        reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                        trip.getId(), ReservationStatus.CANCELLED)
                .stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .forEach(reservation -> notificationService.createForUser(
                        reservation.getUser().getId(),
                        NotificationType.TRIP_STARTED,
                        "Trip started",
                        tripLabel(trip) + " has started.",
                        tripPayload(trip),
                        "trip:" + trip.getId() + ":started:user:" + reservation.getUser().getId()
                ));
    }

    public void tripStationArrivalTimeUpdated(TripStation tripStation) {
        Trip trip = tripStation.getTrip();
        String time = tripStation.getExpectedArrivalTime().format(TIME_FORMAT);
        reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                        trip.getId(), ReservationStatus.CANCELLED)
                .stream()
                .filter(reservation -> usesStation(reservation, tripStation.getId()))
                .forEach(reservation -> notificationService.createForUser(
                        reservation.getUser().getId(),
                        NotificationType.TRIP_STATION_TIME_UPDATED,
                        "Station time updated",
                        "Expected time at " + tripStation.getStation().getName() + " is now " + time + ".",
                        stationPayload(tripStation),
                        "trip-station:" + tripStation.getId() + ":time:" + time
                                + ":user:" + reservation.getUser().getId()
                ));
    }

    /**
     * Sent after the location service confirms the bus is inside a pickup polygon twice in a row.
     * Only confirmed passengers waiting at that pickup station need this arrival alert.
     */
    public void busArrivedAtPickup(Trip trip, Station station) {
        reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                        trip.getId(), ReservationStatus.CANCELLED)
                .stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .filter(reservation -> reservation.getPickupTripStation().getStation().getId().equals(station.getId()))
                .forEach(reservation -> notificationService.createForUser(
                        reservation.getUser().getId(),
                        NotificationType.BUS_ARRIVED_AT_PICKUP,
                        "Bus arrived at your pickup station",
                        "The bus for " + tripLabel(trip) + " has arrived at " + station.getName() + ".",
                        "{\"tripId\":" + trip.getId() + ",\"stationId\":" + station.getId() + "}",
                        "trip:" + trip.getId() + ":pickup-arrival:station:" + station.getId()
                                + ":user:" + reservation.getUser().getId()
                ));
    }

    private boolean usesStation(Reservation reservation, Long tripStationId) {
        return reservation.getPickupTripStation().getId().equals(tripStationId)
                || (reservation.getDropoffTripStation() != null
                && reservation.getDropoffTripStation().getId().equals(tripStationId));
    }

    private String tripLabel(Trip trip) {
        return "trip " + trip.getRoute().getName() + " on " + trip.getServiceDate();
    }

    private String reservationPayload(Reservation reservation) {
        return "{\"reservationId\":" + reservation.getReservationId()
                + ",\"tripId\":" + reservation.getTrip().getId() + "}";
    }

    private String tripPayload(Trip trip) {
        return "{\"tripId\":" + trip.getId() + "}";
    }

    private String stationPayload(TripStation tripStation) {
        return "{\"tripId\":" + tripStation.getTrip().getId()
                + ",\"tripStationId\":" + tripStation.getId()
                + ",\"expectedArrivalTime\":\""
                + tripStation.getExpectedArrivalTime().format(TIME_FORMAT) + "\"}";
    }
}
