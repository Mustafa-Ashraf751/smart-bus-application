package com.verysmartbus.service;

import com.verysmartbus.dto.request.BusAdminTripBusUpdateRequestDto;
import com.verysmartbus.dto.request.LocationUpdateRequestDto;
import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.UserRepository;
import com.verysmartbus.security.BusAdminTripAccessAuthorizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusAdminTripServiceTest {

    private final TripRepository tripRepository = mock(TripRepository.class);
    private final BusRepository busRepository = mock(BusRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BusAdminTripAccessAuthorizer accessAuthorizer = mock(BusAdminTripAccessAuthorizer.class);
    private final TripMapper tripMapper = mock(TripMapper.class);
    private final TripNotificationService tripNotificationService = mock(TripNotificationService.class);
    private final TripService tripService = mock(TripService.class);
    private BusAdminTripService service;

    @BeforeEach
    void setUp() {
        service = new BusAdminTripService(
                tripRepository,
                busRepository,
                userRepository,
                accessAuthorizer,
                tripMapper,
                tripNotificationService,
                tripService
        );
    }

    @Test
    void changesBusForScheduledAssignedTrip() {
        Trip trip = Trip.builder()
                .id(11L)
                .status(TripStatus.SCHEDULED)
                .busAdmin(AppUser.builder().id(5L).build())
                .build();
        Bus activeBus = Bus.builder().id(3L).status(Bus.BusStatus.ACTIVE).build();
        when(accessAuthorizer.findAssignedTrip(5L, 11L)).thenReturn(trip);
        when(busRepository.findById(3L)).thenReturn(Optional.of(activeBus));

        service.updateBus(5L, 11L, new BusAdminTripBusUpdateRequestDto(3L));

        assertEquals(activeBus, trip.getBus());
    }

    @Test
    void rejectsInactiveBus() {
        Trip trip = Trip.builder().id(11L).status(TripStatus.SCHEDULED).build();
        Bus inactiveBus = Bus.builder().id(3L).status(Bus.BusStatus.INACTIVE).build();
        when(accessAuthorizer.findAssignedTrip(5L, 11L)).thenReturn(trip);
        when(busRepository.findById(3L)).thenReturn(Optional.of(inactiveBus));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateBus(5L, 11L, new BusAdminTripBusUpdateRequestDto(3L)));
    }

    @Test
    void letsAssignedBusAdminReportLocationThroughSharedTripLocationLogic() {
        Trip trip = Trip.builder().id(11L).busAdmin(AppUser.builder().id(5L).build()).build();
        LocationUpdateRequestDto request = new LocationUpdateRequestDto(
                new java.math.BigDecimal("30.0444"), new java.math.BigDecimal("31.2357"));
        TripLocationResponseDto expected = new TripLocationResponseDto(11L, null, null, false, List.of());
        when(accessAuthorizer.findAssignedTrip(5L, 11L)).thenReturn(trip);
        when(tripService.updateCurrentLocation(11L, 5L, request)).thenReturn(expected);

        TripLocationResponseDto actual = service.updateCurrentLocation(5L, 11L, request);

        assertSame(expected, actual);
        org.mockito.Mockito.verify(accessAuthorizer).findAssignedTrip(5L, 11L);
        org.mockito.Mockito.verify(tripService).updateCurrentLocation(11L, 5L, request);
    }
}
