package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.RouteStationRequestDto;
import com.verysmartbus.dto.response.RouteStationResponseDto;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.Station;
import org.springframework.stereotype.Component;

@Component
public class RouteStationMapper {

    public RouteStation toEntity(RouteStationRequestDto dto, Route route, Station station) {
        return RouteStation.builder()
                .route(route)
                .station(station)
                .stopOrder(dto.stopOrder())
                .expectedArrivalTime(dto.expectedArrivalTime())
                .build();
    }

    public RouteStationResponseDto toResponseDto(RouteStation entity) {
        return new RouteStationResponseDto(
                entity.getId(),
                entity.getRoute().getId(),
                entity.getStation().getId(),
                entity.getStation().getName(),
                entity.getStopOrder(),
                entity.getExpectedArrivalTime(),
                entity.getCreatedAt()
        );
    }
}
