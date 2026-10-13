package com.verysmartbus.service;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.TripStationStatus;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.mapper.BusMapper;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import com.verysmartbus.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final TripStationRepository tripStationRepository = mock(TripStationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final TripNotificationService tripNotificationService = mock(TripNotificationService.class);
    private final ReservationService service = new ReservationService(
            reservationRepository, tripRepository, tripStationRepository, userRepository,
            reservationMapper, mock(BusMapper.class), tripNotificationService);

    @Test
    void createsWaitlistedReservationWhenTripIsFull() {
        Trip trip = trip(1);
        TripStation pickup = stop(1L, trip, 1);
        TripStation dropoff = stop(2L, trip, 2);
        AppUser passenger = AppUser.builder().id(9L).build();
        Reservation reservation = Reservation.builder().reservationId(50L).user(passenger).trip(trip)
                .pickupTripStation(pickup).dropoffTripStation(dropoff).status(ReservationStatus.WAITLISTED).build();

        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripStationRepository.findById(1L)).thenReturn(Optional.of(pickup));
        when(tripStationRepository.findById(2L)).thenReturn(Optional.of(dropoff));
        when(reservationRepository.existsByUser_IdAndTrip_IdAndStatusNot(9L, 1L, ReservationStatus.CANCELLED))
                .thenReturn(false);
        when(userRepository.findById(any())).thenAnswer(invocation -> Optional.of(passenger));
        when(reservationRepository.countByTrip_IdAndStatus(1L, ReservationStatus.RESERVED)).thenReturn(1L);
        when(reservationMapper.toEntity(any(), any(), any(), any(), any(), any())).thenReturn(reservation);
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponseDto(reservation)).thenReturn(response(reservation));

        ReservationResponseDto result = service.create(9L, new ReservationRequestDto(1L, 1L, 2L));

        assertEquals(ReservationStatus.WAITLISTED, result.status());
        verify(tripNotificationService).reservationCreated(reservation);
    }

    @Test
    void cancellationPromotesTheOldestWaitlistedPassenger() {
        Trip trip = trip(2);
        Reservation confirmed = Reservation.builder().reservationId(60L).user(AppUser.builder().id(9L).build())
                .trip(trip).status(ReservationStatus.RESERVED).build();
        Reservation waitlisted = Reservation.builder().reservationId(61L).user(AppUser.builder().id(10L).build())
                .trip(trip).status(ReservationStatus.WAITLISTED).build();
        when(reservationRepository.findById(60L)).thenReturn(Optional.of(confirmed));
        when(tripRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(trip));
        when(reservationRepository.findFirstByTrip_IdAndStatusOrderByReservedAtAscReservationIdAsc(
                2L, ReservationStatus.WAITLISTED)).thenReturn(Optional.of(waitlisted));
        when(reservationMapper.toResponseDto(confirmed)).thenReturn(response(confirmed));

        service.cancel(9L, 60L);

        assertEquals(ReservationStatus.CANCELLED, confirmed.getStatus());
        assertEquals(ReservationStatus.RESERVED, waitlisted.getStatus());
        verify(tripNotificationService).reservationCancelled(confirmed);
        verify(tripNotificationService).reservationPromoted(waitlisted);
    }

    private Trip trip(long id) {
        return Trip.builder().id(id).route(Route.builder().name("Morning Route").build())
                .serviceDate(LocalDate.now()).status(TripStatus.SCHEDULED)
                .bus(Bus.builder().capacity(1).build()).build();
    }

    private TripStation stop(Long id, Trip trip, int order) {
        return TripStation.builder().id(id).trip(trip).stopOrder(order)
                .station(Station.builder().id(id).name("Station " + id).build())
                .status(TripStationStatus.ACTIVE).build();
    }

    private ReservationResponseDto response(Reservation reservation) {
        return new ReservationResponseDto(reservation.getReservationId(), reservation.getUser().getId(),
                reservation.getTrip().getId(), null, null, null, null, reservation.getStatus(), null, null, null);
    }
}
