package com.verysmartbus.service;

import com.verysmartbus.dto.request.TransportPreferenceRequestDto;
import com.verysmartbus.dto.response.TransportPreferenceResponseDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.dto.response.RouteStationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.TransportPreference;
import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.entity.enums.StationStatus;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.TransportPreferenceMapper;
import com.verysmartbus.mapper.RouteMapper;
import com.verysmartbus.mapper.RouteStationMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.TransportPreferenceRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportPreferenceService {

    private final TransportPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final RouteStationRepository routeStationRepository;
    private final TransportPreferenceMapper mapper;
    private final RouteMapper routeMapper;
    private final RouteStationMapper routeStationMapper;

    /** Read-only selection data for the passenger preference form. */
    @Transactional(readOnly = true)
    public List<RouteResponseDto> getActiveRoutesForPreference() {
        return routeRepository.findAllByStatusOrderByNameAsc(RouteStatus.ACTIVE).stream()
                .map(routeMapper::toResponseDto)
                .toList();
    }

    /**
     * Returns only stops a passenger can select for a new preference: active route and active
     * physical stations, in the fixed route order.
     */
    @Transactional(readOnly = true)
    public List<RouteStationResponseDto> getActiveRouteStationsForPreference(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new IllegalStateException("Route " + routeId + " is not active.");
        }

        return routeStationRepository.findAllByRoute_IdOrderByStopOrderAsc(routeId).stream()
                .filter(routeStation -> routeStation.getStation().getStatus() == StationStatus.ACTIVE)
                .map(routeStationMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public TransportPreferenceResponseDto create(Long currentUserId, TransportPreferenceRequestDto dto) {
        if (preferenceRepository.existsByUser_IdAndDirection(currentUserId, dto.direction())) {
            throw new IllegalStateException(
                    "You already have a transport preference for direction " + dto.direction() + ". Update it instead.");
        }

        ValidatedStops stops = validateRouteAndStops(dto);

        AppUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        TransportPreference saved = preferenceRepository.save(
                mapper.toEntity(dto, user, stops.route(), stops.pickup(), stops.dropoff()));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public TransportPreferenceResponseDto update(Long currentUserId, Long preferenceId,
                                                 TransportPreferenceRequestDto dto) {
        TransportPreference existing = findOwned(preferenceId, currentUserId);

        if (preferenceRepository.existsByUser_IdAndDirectionAndPreferenceIdNot(
                currentUserId, dto.direction(), preferenceId)) {
            throw new IllegalStateException(
                    "You already have another transport preference for direction " + dto.direction() + ".");
        }

        ValidatedStops stops = validateRouteAndStops(dto);
        mapper.updateEntity(existing, dto, stops.route(), stops.pickup(), stops.dropoff());

        return mapper.toResponseDto(preferenceRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public List<TransportPreferenceResponseDto> getForUser(Long userId) {
        return preferenceRepository.findByUser_Id(userId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public TransportPreferenceResponseDto toggleActive(Long currentUserId, Long preferenceId) {
        TransportPreference existing = findOwned(preferenceId, currentUserId);
        existing.setActive(!existing.getActive());
        return mapper.toResponseDto(preferenceRepository.save(existing));
    }

    @Transactional
    public void delete(Long currentUserId, Long preferenceId) {
        TransportPreference existing = findOwned(preferenceId, currentUserId);
        preferenceRepository.delete(existing);
    }

    // ------------------------------------------------------------------ helpers

    private TransportPreference findOwned(Long preferenceId, Long currentUserId) {
        TransportPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preference", preferenceId));

        if (!preference.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You do not own this transport preference.");
        }
        return preference;
    }

    private ValidatedStops validateRouteAndStops(TransportPreferenceRequestDto dto) {
        Route route = routeRepository.findById(dto.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", dto.routeId()));

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new IllegalStateException("Route " + route.getId() + " is not active.");
        }

        RouteStation pickup = loadStopOnRoute(dto.pickupRouteStationId(), route, "Pickup");
        RouteStation dropoff = loadStopOnRoute(dto.dropoffRouteStationId(), route, "Drop-off");

        if (pickup.getStopOrder() >= dropoff.getStopOrder()) {
            throw new IllegalArgumentException("Pickup stop must come before the drop-off stop on the route.");
        }
        return new ValidatedStops(route, pickup, dropoff);
    }

    private RouteStation loadStopOnRoute(Long routeStationId, Route route, String label) {
        RouteStation stop = routeStationRepository.findById(routeStationId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " route station", routeStationId));

        if (!stop.getRoute().getId().equals(route.getId())) {
            throw new IllegalArgumentException(label + " station does not belong to the selected route.");
        }
        return stop;
    }

    private record ValidatedStops(Route route, RouteStation pickup, RouteStation dropoff) {
    }
}
