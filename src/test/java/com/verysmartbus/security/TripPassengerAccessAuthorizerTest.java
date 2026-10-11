package com.verysmartbus.security;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Permission;
import com.verysmartbus.entity.Role;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripPassengerAccessAuthorizerTest {

    private final TripRepository tripRepository = mock(TripRepository.class);
    private final TripPassengerAccessAuthorizer authorizer = new TripPassengerAccessAuthorizer(tripRepository);

    private final Trip trip = Trip.builder()
            .id(15L)
            .busAdmin(AppUser.builder().id(5L).build())
            .driver(AppUser.builder().id(6L).build())
            .build();

    @Test
    void allowsHoldersOfViewAllReservations() {
        Authentication admin = authenticate(1L, "ADMIN", "VIEW_ALL_RESERVATIONS");

        assertDoesNotThrow(() -> authorizer.authorize(admin, 15L));
    }

    @Test
    void allowsTheTripsBusAdminAndDriver() {
        when(tripRepository.findById(15L)).thenReturn(Optional.of(trip));

        assertDoesNotThrow(() -> authorizer.authorize(authenticate(5L, "BUS_ADMIN"), 15L));
        assertDoesNotThrow(() -> authorizer.authorize(authenticate(6L, "DRIVER"), 15L));
    }

    @Test
    void rejectsOtherUsers() {
        when(tripRepository.findById(15L)).thenReturn(Optional.of(trip));

        assertThrows(AccessDeniedException.class,
                () -> authorizer.authorize(authenticate(7L, "EMPLOYEE", "BOOK_TRIP"), 15L));
        assertThrows(AccessDeniedException.class,
                () -> authorizer.authorize(authenticate(8L, "DRIVER"), 15L));
    }

    private Authentication authenticate(Long userId, String roleName, String... permissionNames) {
        Role role = Role.builder().name(roleName).build();
        for (String permissionName : permissionNames) {
            role.getPermissions().add(Permission.builder().name(permissionName).build());
        }
        AppUser user = AppUser.builder().id(userId).email("user" + userId + "@test.com").roles(Set.of(role)).build();
        UserPrincipal principal = UserPrincipal.from(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
