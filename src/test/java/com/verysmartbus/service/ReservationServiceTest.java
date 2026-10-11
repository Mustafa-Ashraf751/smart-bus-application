package com.verysmartbus.service;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.mapper.BusMapper;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import com.verysmartbus.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final TripStationRepository tripStationRepository = mock(TripStationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BusRepository busRepository = mock(BusRepository.class);
    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final BusMapper busMapper = mock(BusMapper.class);
    private ReservationService service;

    private final Trip trip = Trip.builder()
            .id(15L)
            .status(TripStatus.SCHEDULED)
            .bus(Bus.builder().id(3L).capacity(40).build())
            .build();

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                tripRepository,
                tripStationRepository,
                userRepository,
                busRepository,
                reservationMapper,
                busMapper
        );
        when(tripRepository.findById(15L)).thenReturn(Optional.of(trip));
    }

    @Test
    void reservesTheRequestedTripStation() {
        // Trip station id (41) differs from its station id (5): the request carries the trip station id
        TripStation pickup = TripStation.builder()
                .id(41L)
                .trip(trip)
                .station(Station.builder().id(5L).build())
                .build();
        AppUser user = AppUser.builder().id(7L).build();
        when(tripStationRepository.findById(41L)).thenReturn(Optional.of(pickup));
        when(reservationRepository.findFirstByUser_Id(7L)).thenReturn(Optional.empty());
        // Boxed: UserRepository also declares findById(long), the service calls findById(Long)
        when(userRepository.findById(Long.valueOf(7L))).thenReturn(Optional.of(user));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(7L, new ReservationRequestDto(15L, 41L));

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        assertEquals(pickup, saved.getValue().getPickupTripStation());
        assertEquals(ReservationStatus.RESERVED, saved.getValue().getStatus());
    }

    @Test
    void rejectsTripStationOfAnotherTrip() {
        TripStation otherTripStation = TripStation.builder()
                .id(41L)
                .trip(Trip.builder().id(99L).build())
                .build();
        when(tripStationRepository.findById(41L)).thenReturn(Optional.of(otherTripStation));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(7L, new ReservationRequestDto(15L, 41L)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownTripStation() {
        when(tripStationRepository.findById(41L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.create(7L, new ReservationRequestDto(15L, 41L)));
        verify(reservationRepository, never()).save(any());
    }
}
