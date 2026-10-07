package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.RouteRequestDto;
import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.entity.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouteMapper {

    private final LocationMapper locationMapper;

    public Route toEntity(RouteRequestDto dto) {
        return Route.builder()
                .name(dto.name())
                .status(dto.status())
                .build();
    }

    public void updateEntity(Route entity, RouteRequestDto dto) {
        entity.setName(dto.name());
        entity.setStatus(dto.status());
    }

    public RouteResponseDto toResponseDto(Route entity) {
        return new RouteResponseDto(
                entity.getId(),
                entity.getName(),
                locationMapper.toResponseDto(entity.getPath()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
