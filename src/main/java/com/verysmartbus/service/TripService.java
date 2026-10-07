package com.verysmartbus.service;

import com.verysmartbus.dto.request.DriverLocationUpdateRequestDto;
import com.verysmartbus.dto.request.TripRequestDto;
import com.verysmartbus.dto.response.StationPresenceResponseDto;
import com.verysmartbus.dto.response.TripLocationResponseDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStationPresence;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.event.TripLocationUpdatedEvent;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.mapper.LocationMapper;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.StationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationPresenceRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {

    private static final String DRIVER_ROLE_NAME = "DRIVER";
    private static final int ARRIVAL_CONFIRMATION_REQUIRED_UPDATES = 2;

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final TripStationPresenceRepository tripStationPresenceRepository;
    private final LocationFreshnessService locationFreshnessService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TripMapper mapper;
    private final LocationMapper locationMapper;

    public List<TripResponseDto> findAll() {
        return tripRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public List<TripResponseDto> findAllByRouteId(Long routeId) {
        findRouteById(routeId);
        return tripRepository.findAllByRoute_Id(routeId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public TripResponseDto findById(Long tripId) {
        return mapper.toResponseDto(findEntityById(tripId));
    }

    public TripLocationResponseDto findCurrentLocation(Long tripId) {
        Trip trip = findEntityById(tripId);
        List<StationPresenceResponseDto> stationsContainingBus = tripStationPresenceRepository
                .findAllByTrip_Id(tripId)
                .stream()
                .filter(presence -> presence.getConsecutiveInsideCount() > 0)
                .map(presence -> new StationPresenceResponseDto(
                        presence.getStation().getId(),
                        presence.getStation().getName(),
                        presence.getConsecutiveInsideCount(),
                        presence.isArrivalConfirmed()
                ))
                .toList();

        return toLocationResponse(trip, stationsContainingBus);
    }

    @Transactional
    public TripResponseDto create(TripRequestDto dto) {
        Route route = findRouteById(dto.routeId());
        Bus bus = findBusById(dto.busId());
        AppUser driver = findDriverById(dto.driverId());

        Trip saved = tripRepository.save(mapper.toEntity(dto, route, bus, driver));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public TripLocationResponseDto updateCurrentLocation(
            Long tripId,
            Long authenticatedDriverId,
            DriverLocationUpdateRequestDto dto
    ) {
        Trip trip = findEntityById(tripId);
        if (!trip.getDriver().getId().equals(authenticatedDriverId)) {
            throw new ForbiddenOperationException("You can update only your own assigned trip.");
        }
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Location can be updated only while a trip is in progress.");
        }

        trip.setCurrentLocation(locationMapper.toPoint(dto.latitude(), dto.longitude()));
        trip.setLocationUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        List<Station> stationsContainingBus = stationRepository
                .findActiveStationsForTripContaining(tripId, dto.latitude(), dto.longitude());

        List<TripStationPresence> existingPresences = tripStationPresenceRepository.findAllByTrip_Id(tripId);
        Map<Long, TripStationPresence> presenceByStationId = existingPresences.stream()
                .collect(Collectors.toMap(
                        presence -> presence.getStation().getId(),
                        Function.identity()
                ));
        Set<Long> stationIdsContainingBus = stationsContainingBus.stream()
                .map(Station::getId)
                .collect(Collectors.toSet());

        existingPresences.stream()
                .filter(presence -> !stationIdsContainingBus.contains(presence.getStation().getId()))
                .forEach(presence -> resetPresence(presence, trip.getLocationUpdatedAt()));

        List<StationPresenceResponseDto> stationPresenceResponses = stationsContainingBus.stream()
                .map(station -> updatePresence(trip, station, trip.getLocationUpdatedAt(), presenceByStationId))
                .map(presence -> new StationPresenceResponseDto(
                        presence.getStation().getId(),
                        presence.getStation().getName(),
                        presence.getConsecutiveInsideCount(),
                        presence.isArrivalConfirmed()
                ))
                .toList();

        TripLocationResponseDto response = toLocationResponse(trip, stationPresenceResponses);
        applicationEventPublisher.publishEvent(new TripLocationUpdatedEvent(response));
        return response;
    }

    @Transactional
    public TripResponseDto start(Long tripId) {
        Trip trip = findEntityById(tripId);
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled trip can be started.");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualStartTime(OffsetDateTime.now(ZoneOffset.UTC));
        return mapper.toResponseDto(trip);
    }

    @Transactional
    public TripResponseDto complete(Long tripId) {
        Trip trip = findEntityById(tripId);
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress trip can be completed.");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setActualEndTime(OffsetDateTime.now(ZoneOffset.UTC));
        return mapper.toResponseDto(trip);
    }

    @Transactional
    public void delete(Long tripId) {
        tripRepository.delete(findEntityById(tripId));
    }

    private Trip findEntityById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    private Route findRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
    }

    private Bus findBusById(Long busId) {
        return busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));
    }

    private AppUser findDriverById(Long driverId) {
        AppUser driver = userRepository.findByIdWithRoles(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));

        boolean hasDriverRole = driver.getRoles().stream()
                .anyMatch(role -> DRIVER_ROLE_NAME.equals(role.getName()));
        if (!hasDriverRole) {
            throw new IllegalArgumentException("Assigned user must have the DRIVER role.");
        }

        return driver;
    }

    private TripStationPresence updatePresence(
            Trip trip,
            Station station,
            OffsetDateTime observedAt,
            Map<Long, TripStationPresence> presenceByStationId
    ) {
        TripStationPresence presence = presenceByStationId.get(station.getId());
        if (presence == null) {
            presence = TripStationPresence.builder()
                    .trip(trip)
                    .station(station)
                    .build();
        }

        int nextInsideCount = Math.min(
                presence.getConsecutiveInsideCount() + 1,
                ARRIVAL_CONFIRMATION_REQUIRED_UPDATES
        );
        presence.setConsecutiveInsideCount(nextInsideCount);
        presence.setArrivalConfirmed(nextInsideCount >= ARRIVAL_CONFIRMATION_REQUIRED_UPDATES);
        presence.setLastObservedAt(observedAt);

        return tripStationPresenceRepository.save(presence);
    }

    private void resetPresence(TripStationPresence presence, OffsetDateTime observedAt) {
        presence.setConsecutiveInsideCount(0);
        presence.setArrivalConfirmed(false);
        presence.setLastObservedAt(observedAt);
    }

    private TripLocationResponseDto toLocationResponse(
            Trip trip,
            List<StationPresenceResponseDto> stationsContainingBus
    ) {
        return new TripLocationResponseDto(
                trip.getId(),
                locationMapper.toResponseDto(trip.getCurrentLocation()),
                trip.getLocationUpdatedAt(),
                locationFreshnessService.isStale(trip.getLocationUpdatedAt()),
                stationsContainingBus
        );
    }
}
