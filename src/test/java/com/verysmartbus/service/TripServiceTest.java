package com.verysmartbus.service;

import com.verysmartbus.dto.request.LocationUpdateRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStationPresence;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.mapper.LocationMapper;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.mapper.TripStationMapper;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.StationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationPresenceRepository;
import com.verysmartbus.repository.TripStationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TripServiceTest {

    @Test
    void listsTripsRelevantToTheLoggedInUser() {
        TripRepository tripRepository = mock(TripRepository.class);
        TripMapper tripMapper = mock(TripMapper.class);
        TripService service = newTripService(tripRepository, tripMapper);
        Trip trip = Trip.builder().id(50L).build();
        TripResponseDto response = tripResponse(50L);
        when(tripRepository.findRelevantTripsForUser(5L)).thenReturn(List.of(trip));
        when(tripMapper.toResponseDto(trip)).thenReturn(response);

        assertEquals(List.of(response), service.findMyTrips(5L));
    }

    @Test
    void letsAssignedBusAdminCompleteAnInProgressTrip() {
        TripRepository tripRepository = mock(TripRepository.class);
        TripMapper tripMapper = mock(TripMapper.class);
        TripService service = newTripService(tripRepository, tripMapper);
        Trip trip = Trip.builder()
                .id(50L)
                .driver(AppUser.builder().id(7L).build())
                .busAdmin(AppUser.builder().id(5L).build())
                .serviceDate(LocalDate.now(ZoneId.of("Africa/Cairo")))
                .status(TripStatus.IN_PROGRESS)
                .build();
        TripResponseDto response = tripResponse(50L);
        when(tripRepository.findById(50L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);
        when(tripMapper.toResponseDto(trip)).thenReturn(response);

        assertEquals(response, service.completeTrip(5L, 50L));
        assertEquals(TripStatus.COMPLETED, trip.getStatus());
    }

    @Test
    void sendsPickupArrivalNotificationOnlyAfterTwoConsecutiveInsidePolygonUpdates() {
        TripRepository tripRepository = mock(TripRepository.class);
        StationRepository stationRepository = mock(StationRepository.class);
        TripStationPresenceRepository presenceRepository = mock(TripStationPresenceRepository.class);
        LocationFreshnessService locationFreshnessService = mock(LocationFreshnessService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        TripNotificationService tripNotificationService = mock(TripNotificationService.class);

        TripService service = new TripService(
                tripRepository,
                mock(RouteRepository.class),
                mock(RouteStationRepository.class),
                stationRepository,
                mock(TripStationRepository.class),
                presenceRepository,
                reservationRepository,
                locationFreshnessService,
                eventPublisher,
                mock(TripMapper.class),
                mock(TripStationMapper.class),
                new LocationMapper(),
                tripNotificationService
        );

        Trip trip = Trip.builder()
                .id(50L)
                .driver(AppUser.builder().id(7L).build())
                .busAdmin(AppUser.builder().id(5L).build())
                .status(TripStatus.IN_PROGRESS)
                .build();
        Station pickupStation = Station.builder().id(9L).name("Central Station").build();
        AtomicReference<List<TripStationPresence>> storedPresences = new AtomicReference<>(List.of());
        LocationUpdateRequestDto location = new LocationUpdateRequestDto(
                new BigDecimal("30.0444"), new BigDecimal("31.2357"));

        when(tripRepository.findById(50L)).thenReturn(Optional.of(trip));
        when(stationRepository.findActiveStationsForTripContaining(
                eq(50L), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(List.of(pickupStation));
        when(presenceRepository.findAllByTrip_Id(50L)).thenAnswer(invocation -> storedPresences.get());
        when(presenceRepository.save(any(TripStationPresence.class)))
                .thenAnswer(invocation -> {
                    TripStationPresence saved = invocation.getArgument(0);
                    storedPresences.set(List.of(saved));
                    return saved;
                });
        when(locationFreshnessService.isStale(any())).thenReturn(false);

        // First spatial hit is deliberately not enough to trust GPS noise near a polygon edge.
        service.updateCurrentLocation(50L, 5L, location);
        verify(tripNotificationService, never()).busArrivedAtPickup(any(), any());

        // The second consecutive hit confirms arrival and emits the pickup notification once.
        service.updateCurrentLocation(50L, 5L, location);
        verify(tripNotificationService).busArrivedAtPickup(trip, pickupStation);
    }

    private TripService newTripService(TripRepository tripRepository, TripMapper tripMapper) {
        return new TripService(
                tripRepository,
                mock(RouteRepository.class),
                mock(RouteStationRepository.class),
                mock(StationRepository.class),
                mock(TripStationRepository.class),
                mock(TripStationPresenceRepository.class),
                mock(ReservationRepository.class),
                mock(LocationFreshnessService.class),
                mock(ApplicationEventPublisher.class),
                tripMapper,
                mock(TripStationMapper.class),
                new LocationMapper(),
                mock(TripNotificationService.class)
        );
    }

    private TripResponseDto tripResponse(Long tripId) {
        return new TripResponseDto(
                tripId, null, null, null, null, null, null, null, null,
                null, null, false, null, null, null);
    }
}
