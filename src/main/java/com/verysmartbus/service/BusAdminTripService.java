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
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.UserRepository;
import com.verysmartbus.security.BusAdminTripAccessAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BusAdminTripService {

    private static final String DRIVER_ROLE_NAME = "DRIVER";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Cairo");

    private final TripRepository tripRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final BusAdminTripAccessAuthorizer accessAuthorizer;
    private final TripMapper tripMapper;

    public List<TripResponseDto> findMyTrips(Long busAdminId) {
        return tripRepository.findAllByBusAdmin_IdOrderByServiceDateAscScheduledStartTimeAsc(busAdminId)
                .stream()
                .map(tripMapper::toResponseDto)
                .toList();
    }

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

    @Transactional
    public TripResponseDto start(Long busAdminId, Long tripId) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripRunsToday(trip);
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled trip can be started.");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualStartTime(OffsetDateTime.now(ZoneOffset.UTC));
        return tripMapper.toResponseDto(trip);
    }

    @Transactional
    public TripResponseDto complete(Long busAdminId, Long tripId) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripRunsToday(trip);
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress trip can be completed.");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setActualEndTime(OffsetDateTime.now(ZoneOffset.UTC));
        return tripMapper.toResponseDto(trip);
    }

    private void ensureTripCanBeChanged(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Bus and driver can be changed only while a trip is scheduled.");
        }
    }

    private void ensureTripRunsToday(Trip trip) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (!trip.getServiceDate().equals(today)) {
            throw new IllegalStateException("This trip can be operated only on its service date: "
                    + trip.getServiceDate() + ".");
        }
    }
}
