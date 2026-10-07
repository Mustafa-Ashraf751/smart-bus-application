package com.verysmartbus.service;

import com.verysmartbus.dto.request.RouteRequestDto;
import com.verysmartbus.dto.request.RoutePathRequestDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.entity.Route;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.RouteMapper;
import com.verysmartbus.mapper.LocationMapper;
import com.verysmartbus.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.locationtech.jts.geom.LineString;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper mapper;
    private final LocationMapper locationMapper;

    public List<RouteResponseDto> findAll() {
        return routeRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    public RouteResponseDto findById(Long routeId) {
        return mapper.toResponseDto(findEntityById(routeId));
    }

    @Transactional
    public RouteResponseDto create(RouteRequestDto dto) {
        Route saved = routeRepository.save(mapper.toEntity(dto));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public RouteResponseDto update(Long routeId, RouteRequestDto dto) {
        Route route = findEntityById(routeId);
        mapper.updateEntity(route, dto);
        return mapper.toResponseDto(route);
    }

    @Transactional
    public RouteResponseDto updatePath(Long routeId, RoutePathRequestDto dto) {
        Route route = findEntityById(routeId);
        LineString path = locationMapper.toLineString(dto.points());
        if (path.getLength() == 0) {
            throw new IllegalArgumentException("A route path must contain two distinct points.");
        }

        route.setPath(path);
        return mapper.toResponseDto(route);
    }

    @Transactional
    public void delete(Long routeId) {
        routeRepository.delete(findEntityById(routeId));
    }

    private Route findEntityById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
    }
}
