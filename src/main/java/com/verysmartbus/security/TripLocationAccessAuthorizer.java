package com.verysmartbus.security;

import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripLocationAccessAuthorizer {

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public void authorize(Authentication authentication, Long tripId) {
        UserPrincipal principal = getPrincipal(authentication);
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return;
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        if (trip.getDriver().getId().equals(principal.getUserId())) {
            return;
        }

        boolean hasActiveReservation = reservationRepository
                .existsByUser_IdAndTrip_IdAndStatus(
                        principal.getUserId(),
                        tripId,
                        ReservationStatus.RESERVED
                );
        if (hasActiveReservation) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to receive this trip's location updates.");
    }

    private UserPrincipal getPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Trip location access requires an authenticated user.");
        }
        return principal;
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
