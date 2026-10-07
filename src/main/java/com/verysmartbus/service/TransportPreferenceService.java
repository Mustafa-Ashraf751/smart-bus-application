package com.verysmartbus.service;

import com.verysmartbus.dto.request.TransportPreferenceRequestDto;
import com.verysmartbus.dto.response.TransportPreferenceResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.TransportPreference;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.mapper.TransportPreferenceMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.StationRepository;
import com.verysmartbus.repository.TransportPreferenceRepository;
import com.verysmartbus.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class TransportPreferenceService {

    private final TransportPreferenceRepository transportPreferenceRepository;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final TransportPreferenceMapper mapper;

    @Transactional
    public TransportPreferenceResponseDto create(Long currentUserId, TransportPreferenceRequestDto dto) {

        if (transportPreferenceRepository.existsByUser_IdAndDirection(currentUserId, dto.direction())) {
            throw new IllegalStateException(
                    "A transport preference for direction " + dto.direction() + " already exists for this user.");
        }

        AppUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        Route route = routeRepository.findById(dto.routeId())
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + dto.routeId()));
        Station station = stationRepository.findById(dto.stationId())
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + dto.stationId()));

        TransportPreference entity = mapper.toEntity(dto, user, route, station);
        TransportPreference saved = transportPreferenceRepository.save(entity);

        return mapper.toResponseDto(saved);
    }

    @Transactional
    public TransportPreferenceResponseDto update(Long currentUserId, Long preferenceId, TransportPreferenceRequestDto dto) {
        TransportPreference existing = transportPreferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Preference not found: " + preferenceId));

        assertOwnership(existing, currentUserId);

        Route route = routeRepository.findById(dto.routeId())
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + dto.routeId()));
        Station station = stationRepository.findById(dto.stationId())
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + dto.stationId()));

        mapper.updateEntity(existing, dto, route, station);

        return mapper.toResponseDto(existing);
    }

    @Transactional(readOnly = true)
    public List<TransportPreferenceResponseDto> getForUser(Long userId) {
        return transportPreferenceRepository.findByUser_Id(userId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public void delete(Long currentUserId, Long preferenceId) {
        TransportPreference existing = transportPreferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Preference not found: " + preferenceId));

        assertOwnership(existing, currentUserId);

        transportPreferenceRepository.delete(existing);
    }

    private void assertOwnership(TransportPreference preference, Long currentUserId) {
        if (!preference.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You do not own this transport preference.");
        }
    }

    @Transactional
    public TransportPreferenceResponseDto toggleActive(Long currentUserId, Long preferenceId) {
        TransportPreference existing = transportPreferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Preference not found: " + preferenceId));

        assertOwnership(existing, currentUserId);

        existing.setActive(!existing.getActive());

        return mapper.toResponseDto(existing);
    }
}
