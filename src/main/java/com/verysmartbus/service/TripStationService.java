package com.verysmartbus.service;

import com.verysmartbus.dto.request.TripStationRequestDto;
import com.verysmartbus.dto.response.TripStationResponseDto;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.TripStationMapper;
import com.verysmartbus.repository.StationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripStationService {

    private final TripStationRepository tripStationRepository;
    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final TripStationMapper mapper;

    public List<TripStationResponseDto> findAllByTripId(Long tripId) {
        findTripById(tripId);
        return tripStationRepository.findAllByTrip_IdOrderByStopOrderAsc(tripId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public TripStationResponseDto findById(Long tripStationId) {
        return mapper.toResponseDto(findEntityById(tripStationId));
    }

    @Transactional
    public TripStationResponseDto addStation(TripStationRequestDto dto) {
        if (tripStationRepository.existsByTrip_IdAndStation_Id(dto.tripId(), dto.stationId())) {
            throw new IllegalArgumentException("This station is already assigned to the trip");
        }

        if (tripStationRepository.existsByTrip_IdAndStopOrder(dto.tripId(), dto.stopOrder())) {
            throw new IllegalArgumentException("This stop order is already used for the trip");
        }

        Trip trip = findTripById(dto.tripId());
        Station station = findStationById(dto.stationId());

        TripStation saved = tripStationRepository.save(mapper.toEntity(dto, trip, station));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public void delete(Long tripStationId) {
        tripStationRepository.delete(findEntityById(tripStationId));
    }

    private TripStation findEntityById(Long tripStationId) {
        return tripStationRepository.findById(tripStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip station", tripStationId));
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    private Station findStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station", stationId));
    }
}
