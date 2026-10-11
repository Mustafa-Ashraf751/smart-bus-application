package com.verysmartbus.dto.request;

import com.verysmartbus.entity.enums.Direction;
import jakarta.validation.constraints.NotNull;

/**
 * pickupRouteStationId / dropoffRouteStationId are IDs from the route_stations table
 * (not station IDs). "active" is optional and defaults to true on create.
 */
public record TransportPreferenceRequestDto(
        @NotNull Long routeId,
        @NotNull Long pickupRouteStationId,
        @NotNull Long dropoffRouteStationId,
        @NotNull Direction direction,
        Boolean active
) {
}
