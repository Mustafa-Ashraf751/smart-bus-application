package com.verysmartbus.service;

import com.verysmartbus.dto.request.RouteRequestDto;
import com.verysmartbus.dto.request.RoutePathRequestDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Bus.BusStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.RouteMapper;
import com.verysmartbus.mapper.LocationMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.locationtech.jts.geom.LineString;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RouteService {

    private static final String DRIVER_ROLE_NAME = "DRIVER";
    private static final String BUS_ADMIN_ROLE_NAME = "BUS_ADMIN";

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
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
        Bus defaultBus = findActiveBusById(dto.defaultBusId());
        AppUser defaultDriver = findUserWithRole(dto.defaultDriverId(), DRIVER_ROLE_NAME, "Driver");
        AppUser defaultBusAdmin = findUserWithRole(dto.defaultBusAdminId(), BUS_ADMIN_ROLE_NAME, "Bus admin");
        Route saved = routeRepository.save(mapper.toEntity(dto, defaultBus, defaultDriver, defaultBusAdmin));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public RouteResponseDto update(Long routeId, RouteRequestDto dto) {
        Route route = findEntityById(routeId);
        Bus defaultBus = findActiveBusById(dto.defaultBusId());
        AppUser defaultDriver = findUserWithRole(dto.defaultDriverId(), DRIVER_ROLE_NAME, "Driver");
        AppUser defaultBusAdmin = findUserWithRole(dto.defaultBusAdminId(), BUS_ADMIN_ROLE_NAME, "Bus admin");
        mapper.updateEntity(route, dto, defaultBus, defaultDriver, defaultBusAdmin);
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

    private Bus findActiveBusById(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));
        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new IllegalArgumentException("The default bus must be active.");
        }
        return bus;
    }

    private AppUser findUserWithRole(Long userId, String roleName, String resourceName) {
        AppUser user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, userId));
        boolean hasRequiredRole = user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
        if (!hasRequiredRole) {
            throw new IllegalArgumentException(resourceName + " must have the " + roleName + " role.");
        }
        return user;
    }
}
