package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.TripStationRequestDto;
import com.verysmartbus.dto.response.TripStationResponseDto;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.Station;
import org.springframework.stereotype.Component;

@Component
public class TripStationMapper {

    public TripStation toEntity(TripStationRequestDto dto, Trip trip, Station station) {
        return TripStation.builder()
                .trip(trip)
                .station(station)
                .stopOrder(dto.stopOrder())
                .build();
    }

    public TripStationResponseDto toResponseDto(TripStation entity) {
        return new TripStationResponseDto(
                entity.getId(),
                entity.getTrip().getId(),
                entity.getStation().getId(),
                entity.getStation().getName(),
                entity.getStopOrder(),
                entity.getCreatedAt()
        );
    }
}
