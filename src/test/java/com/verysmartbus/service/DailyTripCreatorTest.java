package com.verysmartbus.service;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.TransportPreference;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.mapper.TripStationMapper;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.TransportPreferenceRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyTripCreatorTest {

    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final RouteStationRepository routeStationRepository = mock(RouteStationRepository.class);
    private final TripStationRepository tripStationRepository = mock(TripStationRepository.class);
    private final TransportPreferenceRepository preferenceRepository = mock(TransportPreferenceRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final DailyTripCreator creator = new DailyTripCreator(routeRepository, tripRepository,
            routeStationRepository, tripStationRepository, new TripStationMapper(), preferenceRepository,
            reservationRepository, reservationMapper);

    @Test
    void doesNothingWhenTheDailyTripAlreadyExists() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        when(tripRepository.existsByRoute_IdAndServiceDate(1L, date)).thenReturn(true);

        assertFalse(creator.createIfMissing(1L, date));

        verify(routeRepository, never()).findById(any());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void skipsRouteThatIsNotReady() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        Route inactive = Route.builder().id(1L).status(RouteStatus.INACTIVE).build();
        when(tripRepository.existsByRoute_IdAndServiceDate(1L, date)).thenReturn(false);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertFalse(creator.createIfMissing(1L, date));

        verify(tripRepository, never()).save(any());
    }

    @Test
    void copiesRouteStationsAndMakesPreferenceReservationsUsingCapacityOrder() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        Station firstStation = Station.builder().id(1L).name("First").build();
        Station secondStation = Station.builder().id(2L).name("Second").build();
        Route route = Route.builder().id(1L).status(RouteStatus.ACTIVE)
                .defaultDepartureTime(LocalTime.of(8, 0))
                .defaultBus(Bus.builder().capacity(1).build())
                .defaultDriver(AppUser.builder().id(7L).build()).build();
        RouteStation first = RouteStation.builder().route(route).station(firstStation).stopOrder(1)
                .expectedArrivalTime(LocalTime.of(8, 0)).build();
        RouteStation second = RouteStation.builder().route(route).station(secondStation).stopOrder(2)
                .expectedArrivalTime(LocalTime.of(8, 15)).build();
        TransportPreference preference = TransportPreference.builder().preferenceId(99L)
                .user(AppUser.builder().id(10L).build()).route(route)
                .pickupRouteStation(first).dropoffRouteStation(second).active(true).build();
        TransportPreference laterPreference = TransportPreference.builder().preferenceId(100L)
                .user(AppUser.builder().id(11L).build()).route(route)
                .pickupRouteStation(first).dropoffRouteStation(second).active(true).build();

        when(tripRepository.existsByRoute_IdAndServiceDate(1L, date)).thenReturn(false);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStationRepository.findAllByRoute_IdOrderByStopOrderAsc(1L)).thenReturn(List.of(first, second));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(100L);
            return trip;
        });
        when(tripStationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(preferenceRepository.findByRoute_IdAndActiveTrueOrderByCreatedAtAscPreferenceIdAsc(1L))
                .thenReturn(List.of(preference, laterPreference));
        when(reservationMapper.toEntity(eq(preference.getUser()), any(Trip.class), any(TripStation.class),
                any(TripStation.class), eq(ReservationStatus.RESERVED), eq(ReservationSource.PREFERENCE)))
                .thenReturn(Reservation.builder().build());

        assertTrue(creator.createIfMissing(1L, date));

        verify(tripStationRepository).saveAll(any());
        verify(reservationMapper).toEntity(eq(preference.getUser()), any(Trip.class), any(TripStation.class),
                any(TripStation.class), eq(ReservationStatus.RESERVED), eq(ReservationSource.PREFERENCE));
        verify(reservationMapper).toEntity(eq(laterPreference.getUser()), any(Trip.class), any(TripStation.class),
                any(TripStation.class), eq(ReservationStatus.WAITLISTED), eq(ReservationSource.PREFERENCE));
        verify(reservationRepository).saveAll(any());
    }
}
