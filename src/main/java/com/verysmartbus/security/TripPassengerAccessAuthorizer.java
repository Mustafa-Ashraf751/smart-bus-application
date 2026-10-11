package com.verysmartbus.security;

import com.verysmartbus.entity.Trip;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may see a trip's passenger list: holders of VIEW_ALL_RESERVATIONS (admins),
 * and the bus admin and driver assigned to that trip.
 */
@Service
@RequiredArgsConstructor
public class TripPassengerAccessAuthorizer {

    private static final String VIEW_ALL_RESERVATIONS = "VIEW_ALL_RESERVATIONS";

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public void authorize(Authentication authentication, Long tripId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Passenger lists require an authenticated user.");
        }
        boolean canViewAll = authentication.getAuthorities().stream()
                .anyMatch(authority -> VIEW_ALL_RESERVATIONS.equals(authority.getAuthority()));
        if (canViewAll) {
            return;
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        Long userId = principal.getUserId();
        if (trip.getBusAdmin() != null && trip.getBusAdmin().getId().equals(userId)) {
            return;
        }
        if (trip.getDriver() != null && trip.getDriver().getId().equals(userId)) {
            return;
        }

        throw new AccessDeniedException("You can view passengers only for trips you operate.");
    }
}
