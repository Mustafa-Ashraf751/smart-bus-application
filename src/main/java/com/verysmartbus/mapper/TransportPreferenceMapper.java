package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.TransportPreferenceRequestDto;
import com.verysmartbus.dto.response.TransportPreferenceResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.TransportPreference;
import org.springframework.stereotype.Component;

@Component
public class TransportPreferenceMapper {

    public TransportPreference toEntity(TransportPreferenceRequestDto dto,
                                        AppUser user,
                                        Route route,
                                        RouteStation pickup,
                                        RouteStation dropoff) {
        return TransportPreference.builder()
                .user(user)
                .route(route)
                .pickupRouteStation(pickup)
                .dropoffRouteStation(dropoff)
                .direction(dto.direction())
                .active(dto.active() == null ? Boolean.TRUE : dto.active())
                .build();
    }

    public void updateEntity(TransportPreference entity,
                             TransportPreferenceRequestDto dto,
                             Route route,
                             RouteStation pickup,
                             RouteStation dropoff) {
        entity.setRoute(route);
        entity.setPickupRouteStation(pickup);
        entity.setDropoffRouteStation(dropoff);
        entity.setDirection(dto.direction());
        if (dto.active() != null) {
            entity.setActive(dto.active());
        }
    }

    public TransportPreferenceResponseDto toResponseDto(TransportPreference entity) {
        RouteStation pickup = entity.getPickupRouteStation();
        RouteStation dropoff = entity.getDropoffRouteStation();

        // pickup/dropoff can be null only for legacy rows created before the route-station model.
        return new TransportPreferenceResponseDto(
                entity.getPreferenceId(),
                entity.getUser().getId(),
                entity.getRoute().getId(),
                entity.getRoute().getName(),
                pickup == null ? null : pickup.getId(),
                pickup == null ? null : pickup.getStation().getName(),
                dropoff == null ? null : dropoff.getId(),
                dropoff == null ? null : dropoff.getStation().getName(),
                entity.getDirection(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
