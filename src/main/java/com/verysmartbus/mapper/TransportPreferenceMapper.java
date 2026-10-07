package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.TransportPreferenceRequestDto;
import com.verysmartbus.dto.response.TransportPreferenceResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.TransportPreference;
import org.springframework.stereotype.Component;


@Component
public class TransportPreferenceMapper {

    public TransportPreference toEntity(TransportPreferenceRequestDto dto, AppUser user, Route route, Station station) {
        return TransportPreference.builder()
                .user(user)
                .route(route)
                .station(station)
                .direction(dto.direction())
                .build();
    }


    public void updateEntity(TransportPreference entity, TransportPreferenceRequestDto dto, Route route, Station station) {
        entity.setRoute(route);
        entity.setStation(station);
        entity.setDirection(dto.direction());
    }

    public TransportPreferenceResponseDto toResponseDto(TransportPreference entity) {
        return new TransportPreferenceResponseDto(
                entity.getPreferenceId(),
                entity.getUser().getId(),
                entity.getRoute().getId(),
                entity.getRoute().getName(),
                entity.getStation().getId(),
                entity.getStation().getName(),
                entity.getDirection(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
