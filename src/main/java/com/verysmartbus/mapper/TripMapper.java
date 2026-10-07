package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.TripRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.service.LocationFreshnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripMapper {

    private final LocationMapper locationMapper;
    private final LocationFreshnessService locationFreshnessService;

    public Trip toEntity(TripRequestDto dto, Route route, Bus bus, AppUser driver) {
        return Trip.builder()
                .route(route)
                .bus(bus)
                .driver(driver)
                .scheduledStartTime(dto.scheduledStartTime())
                .build();
    }

    public TripResponseDto toResponseDto(Trip entity) {
        return new TripResponseDto(
                entity.getId(),
                entity.getRoute().getId(),
                entity.getBus().getId(),
                entity.getDriver().getId(),
                entity.getScheduledStartTime(),
                entity.getActualStartTime(),
                entity.getActualEndTime(),
                locationMapper.toResponseDto(entity.getCurrentLocation()),
                entity.getLocationUpdatedAt(),
                locationFreshnessService.isStale(entity.getLocationUpdatedAt()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
