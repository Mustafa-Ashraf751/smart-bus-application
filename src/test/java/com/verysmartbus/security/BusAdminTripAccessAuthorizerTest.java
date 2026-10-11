package com.verysmartbus.security;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.repository.TripRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusAdminTripAccessAuthorizerTest {

    private final TripRepository tripRepository = mock(TripRepository.class);
    private final BusAdminTripAccessAuthorizer authorizer = new BusAdminTripAccessAuthorizer(tripRepository);

    @Test
    void returnsTripWhenItBelongsToCurrentBusAdmin() {
        Trip trip = Trip.builder().id(11L).busAdmin(AppUser.builder().id(5L).build()).build();
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));

        Trip result = authorizer.findAssignedTrip(5L, 11L);

        assertSame(trip, result);
    }

    @Test
    void rejectsTripAssignedToAnotherBusAdmin() {
        Trip trip = Trip.builder().id(11L).busAdmin(AppUser.builder().id(5L).build()).build();
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));

        assertThrows(ForbiddenOperationException.class, () -> authorizer.findAssignedTrip(9L, 11L));
    }
}
