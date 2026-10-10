package com.verysmartbus.security;

import com.verysmartbus.entity.Trip;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusAdminTripAccessAuthorizer {

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public Trip findAssignedTrip(Long busAdminId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        if (trip.getBusAdmin() == null || !trip.getBusAdmin().getId().equals(busAdminId)) {
            throw new ForbiddenOperationException("You can manage only trips assigned to you.");
        }
        return trip;
    }
}
