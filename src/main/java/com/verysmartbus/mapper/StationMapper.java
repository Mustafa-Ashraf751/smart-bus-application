package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.StationRequestDto;
import com.verysmartbus.dto.response.MapPointResponseDto;
import com.verysmartbus.dto.response.StationResponseDto;
import com.verysmartbus.entity.Station;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class StationMapper {

    public StationResponseDto toResponseDto(Station entity) {
        return new StationResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                toAreaBoundary(entity),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<MapPointResponseDto> toAreaBoundary(Station entity) {
        return Arrays.stream(entity.getArea().getExteriorRing().getCoordinates())
                .map(this::toMapPoint)
                .toList();
    }

    private MapPointResponseDto toMapPoint(Coordinate coordinate) {
        return new MapPointResponseDto(
                BigDecimal.valueOf(coordinate.getX()),
                BigDecimal.valueOf(coordinate.getY())
        );
    }
}
