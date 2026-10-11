package com.verysmartbus.service;

import com.verysmartbus.dto.request.BusAdminTripBusUpdateRequestDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusAdminTripServiceTest {

    private final TripRepository tripRepository = mock(TripRepository.class);
    private final BusRepository busRepository = mock(BusRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BusAdminTripAccessAuthorizer accessAuthorizer = mock(BusAdminTripAccessAuthorizer.class);
    private final TripMapper tripMapper = mock(TripMapper.class);
    private BusAdminTripService service;

    @BeforeEach
    void setUp() {
        service = new BusAdminTripService(
                tripRepository,
                busRepository,
                userRepository,
                accessAuthorizer,
                tripMapper
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
}
