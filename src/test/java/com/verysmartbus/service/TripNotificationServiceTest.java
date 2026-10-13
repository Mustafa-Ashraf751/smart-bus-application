package com.verysmartbus.service;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.NotificationType;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TripNotificationServiceTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final TripNotificationService service = new TripNotificationService(notificationService, reservationRepository);

    @Test
    void tripStartedNotifiesConfirmedPassengersButNotWaitlistedPassengers() {
        Trip trip = trip();
        Reservation confirmed = reservation(101L, 11L, trip, ReservationStatus.RESERVED);
        Reservation waitlisted = reservation(102L, 12L, trip, ReservationStatus.WAITLISTED);
        when(reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                50L, ReservationStatus.CANCELLED)).thenReturn(List.of(confirmed, waitlisted));

        service.tripStarted(trip);

        verify(notificationService).createForUser(
                eq(11L), eq(NotificationType.TRIP_STARTED), eq("Trip started"),
                contains("Morning Route"), eq("{\"tripId\":50}"), eq("trip:50:started:user:11"));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void stationTimeChangeNotifiesOnlyPassengersUsingThatStation() {
        Trip trip = trip();
        TripStation changedStation = TripStation.builder()
                .id(21L)
                .trip(trip)
                .station(Station.builder().id(9L).name("Central Station").build())
                .expectedArrivalTime(LocalTime.of(13, 34, 44))
                .build();
        Reservation affected = reservation(101L, 11L, trip, ReservationStatus.RESERVED);
        affected.setPickupTripStation(changedStation);
        Reservation unaffected = reservation(102L, 12L, trip, ReservationStatus.RESERVED);
        unaffected.setPickupTripStation(TripStation.builder().id(22L).trip(trip).build());
        when(reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                50L, ReservationStatus.CANCELLED)).thenReturn(List.of(affected, unaffected));

        service.tripStationArrivalTimeUpdated(changedStation);

        verify(notificationService).createForUser(
                eq(11L), eq(NotificationType.TRIP_STATION_TIME_UPDATED), eq("Station time updated"),
                contains("13:34:44"), contains("\"tripStationId\":21"),
                eq("trip-station:21:time:13:34:44:user:11"));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void pickupArrivalNotifiesOnlyConfirmedPassengersWaitingAtThatStation() {
        Trip trip = trip();
        Station pickupStation = Station.builder().id(9L).name("Central Station").build();
        TripStation pickup = TripStation.builder().id(21L).trip(trip).station(pickupStation).build();
        Reservation confirmedAtPickup = reservation(101L, 11L, trip, ReservationStatus.RESERVED);
        confirmedAtPickup.setPickupTripStation(pickup);
        Reservation waitlistedAtPickup = reservation(102L, 12L, trip, ReservationStatus.WAITLISTED);
        waitlistedAtPickup.setPickupTripStation(pickup);
        when(reservationRepository.findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
                50L, ReservationStatus.CANCELLED)).thenReturn(List.of(confirmedAtPickup, waitlistedAtPickup));

        service.busArrivedAtPickup(trip, pickupStation);

        verify(notificationService).createForUser(
                eq(11L), eq(NotificationType.BUS_ARRIVED_AT_PICKUP),
                eq("Bus arrived at your pickup station"), contains("Central Station"),
                eq("{\"tripId\":50,\"stationId\":9}"),
                eq("trip:50:pickup-arrival:station:9:user:11"));
        verifyNoMoreInteractions(notificationService);
    }

    private Trip trip() {
        return Trip.builder()
                .id(50L)
                .route(Route.builder().name("Morning Route").build())
                .serviceDate(LocalDate.of(2026, 10, 13))
                .build();
    }

    private Reservation reservation(Long reservationId, Long userId, Trip trip, ReservationStatus status) {
        return Reservation.builder()
                .reservationId(reservationId)
                .user(AppUser.builder().id(userId).build())
                .trip(trip)
                .status(status)
                .build();
    }
}
