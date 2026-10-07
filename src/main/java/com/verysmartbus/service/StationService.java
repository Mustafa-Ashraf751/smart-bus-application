package com.verysmartbus.service;

import com.verysmartbus.dto.request.StationRequestDto;
import com.verysmartbus.dto.response.StationResponseDto;
import com.verysmartbus.entity.Station;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.StationMapper;
import com.verysmartbus.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final StationMapper mapper;

    public List<StationResponseDto> findAll() {
        return stationRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public StationResponseDto findById(Long stationId) {
        return mapper.toResponseDto(findEntityById(stationId));
    }

    @Transactional
    public StationResponseDto create(StationRequestDto dto) {
        Station saved = stationRepository.createWithPickupArea(
                dto.name(),
                dto.address(),
                dto.status(),
                dto.pickupLatitude(),
                dto.pickupLongitude()
        );
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public StationResponseDto update(Long stationId, StationRequestDto dto) {
        Station updated = stationRepository.updateWithPickupArea(
                stationId,
                dto.name(),
                dto.address(),
                dto.status(),
                dto.pickupLatitude(),
                dto.pickupLongitude()
        );
        if (updated == null) {
            throw new ResourceNotFoundException("Station", stationId);
        }
        return mapper.toResponseDto(updated);
    }

    @Transactional
    public void delete(Long stationId) {
        stationRepository.delete(findEntityById(stationId));
    }

    private Station findEntityById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station", stationId));
    }
}
