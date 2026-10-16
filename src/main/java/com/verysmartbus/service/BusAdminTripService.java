package com.verysmartbus.service;

import com.verysmartbus.dto.request.BusAdminTripBusUpdateRequestDto;
import com.verysmartbus.dto.request.BusAdminTripDriverUpdateRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.UserRepository;
import com.verysmartbus.security.BusAdminTripAccessAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BusAdminTripService {

    private static final String DRIVER_ROLE_NAME = "DRIVER";
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final BusAdminTripAccessAuthorizer accessAuthorizer;
    private final TripMapper tripMapper;

    public TripResponseDto findMyTrip(Long busAdminId, Long tripId) {
        return tripMapper.toResponseDto(accessAuthorizer.findAssignedTrip(busAdminId, tripId));
    }

    @Transactional
    public TripResponseDto updateBus(Long busAdminId, Long tripId, BusAdminTripBusUpdateRequestDto request) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripCanBeChanged(trip);

        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.busId()));
        if (bus.getStatus() != Bus.BusStatus.ACTIVE) {
            throw new IllegalArgumentException("The assigned bus must be active.");
        }

        trip.setBus(bus);
        return tripMapper.toResponseDto(trip);
    }

    @Transactional
    public TripResponseDto updateDriver(Long busAdminId, Long tripId, BusAdminTripDriverUpdateRequestDto request) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripCanBeChanged(trip);

        AppUser driver = userRepository.findByIdWithRoles(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", request.driverId()));
        boolean hasDriverRole = driver.getRoles().stream()
                .anyMatch(role -> DRIVER_ROLE_NAME.equals(role.getName()));
        if (!hasDriverRole) {
            throw new IllegalArgumentException("The assigned user must have the DRIVER role.");
        }

        trip.setDriver(driver);
        return tripMapper.toResponseDto(trip);
    }

    private void ensureTripCanBeChanged(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Bus and driver can be changed only while a trip is scheduled.");
        }
    }

}
