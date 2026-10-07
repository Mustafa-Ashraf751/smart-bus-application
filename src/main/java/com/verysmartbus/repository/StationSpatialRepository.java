package com.verysmartbus.repository;

import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.enums.StationStatus;

import java.math.BigDecimal;

public interface StationSpatialRepository {

    Station createWithPickupArea(
            String name,
            String address,
            StationStatus status,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude
    );

    Station updateWithPickupArea(
            Long stationId,
            String name,
            String address,
            StationStatus status,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude
    );
}
