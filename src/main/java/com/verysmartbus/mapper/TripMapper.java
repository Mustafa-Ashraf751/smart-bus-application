package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.TripRequestDto;
import com.verysmartbus.dto.response.TripResponseDto;
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

    public Trip toEntity(TripRequestDto dto, Route route) {
        return Trip.builder()
                .route(route)
                .bus(route.getDefaultBus())
                .driver(route.getDefaultDriver())
                .busAdmin(route.getDefaultBusAdmin())
                .serviceDate(dto.serviceDate())
                .scheduledStartTime(dto.serviceDate()
                        .atTime(route.getDefaultDepartureTime())
                        .atZone(java.time.ZoneId.of("Africa/Cairo"))
                        .toOffsetDateTime())
                .build();
    }

    public TripResponseDto toResponseDto(Trip entity) {
        return new TripResponseDto(
                entity.getId(),
                entity.getRoute().getId(),
                entity.getBus().getId(),
                entity.getDriver().getId(),
                entity.getBusAdmin() == null ? null : entity.getBusAdmin().getId(),
                entity.getServiceDate(),
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
