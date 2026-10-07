package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.RouteRequestDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouteMapper {

    private final LocationMapper locationMapper;

    public Route toEntity(RouteRequestDto dto, Bus defaultBus, AppUser defaultDriver, AppUser defaultBusAdmin) {
        return Route.builder()
                .name(dto.name())
                .status(dto.status())
                .direction(dto.direction())
                .defaultDepartureTime(dto.defaultDepartureTime())
                .defaultBus(defaultBus)
                .defaultDriver(defaultDriver)
                .defaultBusAdmin(defaultBusAdmin)
                .build();
    }

    public void updateEntity(
            Route entity,
            RouteRequestDto dto,
            Bus defaultBus,
            AppUser defaultDriver,
            AppUser defaultBusAdmin
    ) {
        entity.setName(dto.name());
        entity.setStatus(dto.status());
        entity.setDirection(dto.direction());
        entity.setDefaultDepartureTime(dto.defaultDepartureTime());
        entity.setDefaultBus(defaultBus);
        entity.setDefaultDriver(defaultDriver);
        entity.setDefaultBusAdmin(defaultBusAdmin);
    }

    public RouteResponseDto toResponseDto(Route entity) {
        return new RouteResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDirection(),
                locationMapper.toResponseDto(entity.getPath()),
                entity.getStatus(),
                entity.getDefaultDepartureTime(),
                entity.getDefaultBus() == null ? null : entity.getDefaultBus().getId(),
                entity.getDefaultDriver() == null ? null : entity.getDefaultDriver().getId(),
                entity.getDefaultBusAdmin() == null ? null : entity.getDefaultBusAdmin().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
