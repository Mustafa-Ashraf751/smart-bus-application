package com.verysmartbus.service;

import com.verysmartbus.dto.request.RouteStationRequestDto;
import com.verysmartbus.dto.response.RouteStationResponseDto;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.Station;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.RouteStationMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RouteStationService {

    private final RouteStationRepository routeStationRepository;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final RouteStationMapper mapper;

    public List<RouteStationResponseDto> findAllByRouteId(Long routeId) {
        findRouteById(routeId);
        return routeStationRepository.findAllByRoute_IdOrderByStopOrderAsc(routeId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public RouteStationResponseDto findById(Long routeStationId) {
        return mapper.toResponseDto(findEntityById(routeStationId));
    }

    @Transactional
    public RouteStationResponseDto addStation(RouteStationRequestDto dto) {
        if (routeStationRepository.existsByRoute_IdAndStation_Id(dto.routeId(), dto.stationId())) {
            throw new IllegalArgumentException("This station is already assigned to the route");
        }

        if (routeStationRepository.existsByRoute_IdAndStopOrder(dto.routeId(), dto.stopOrder())) {
            throw new IllegalArgumentException("This stop order is already used for the route");
        }

        Route route = findRouteById(dto.routeId());
        Station station = findStationById(dto.stationId());

        RouteStation saved = routeStationRepository.save(mapper.toEntity(dto, route, station));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public void delete(Long routeStationId) {
        routeStationRepository.delete(findEntityById(routeStationId));
    }

    private RouteStation findEntityById(Long routeStationId) {
        return routeStationRepository.findById(routeStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Route station", routeStationId));
    }

    private Route findRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
    }

    private Station findStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station", stationId));
    }
}
