package com.verysmartbus.repository;

import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.enums.StationStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class StationRepositoryImpl implements StationSpatialRepository {

    private static final int PICKUP_AREA_RADIUS_METERS = 50;

    private final EntityManager entityManager;

    @Override
    public Station createWithPickupArea(
            String name,
            String address,
            StationStatus status,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude
    ) {
        Number stationId = (Number) entityManager.createNativeQuery("""
                INSERT INTO stations (name, address, area, status)
                VALUES (:name, :address,
                        extensions.ST_Buffer(
                                extensions.ST_SetSRID(
                                        extensions.ST_MakePoint(:longitude, :latitude), 4326
                                )::extensions.geography,
                                :radiusMeters
                        )::extensions.geometry,
                        :status
                )
                RETURNING id
                """)
                .setParameter("name", name)
                .setParameter("address", address)
                .setParameter("latitude", pickupLatitude)
                .setParameter("longitude", pickupLongitude)
                .setParameter("radiusMeters", PICKUP_AREA_RADIUS_METERS)
                .setParameter("status", status.name())
                .getSingleResult();

        return entityManager.find(Station.class, stationId.longValue());
    }

    @Override
    public Station updateWithPickupArea(
            Long stationId,
            String name,
            String address,
            StationStatus status,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude
    ) {
        int rowsUpdated = entityManager.createNativeQuery("""
                UPDATE stations
                SET name = :name,
                    address = :address,
                    area = extensions.ST_Buffer(
                            extensions.ST_SetSRID(
                                    extensions.ST_MakePoint(:longitude, :latitude), 4326
                            )::extensions.geography,
                            :radiusMeters
                    )::extensions.geometry,
                    status = :status,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :stationId
                """)
                .setParameter("stationId", stationId)
                .setParameter("name", name)
                .setParameter("address", address)
                .setParameter("latitude", pickupLatitude)
                .setParameter("longitude", pickupLongitude)
                .setParameter("radiusMeters", PICKUP_AREA_RADIUS_METERS)
                .setParameter("status", status.name())
                .executeUpdate();

        if (rowsUpdated == 0) {
            return null;
        }

        entityManager.clear();
        return entityManager.find(Station.class, stationId);
    }
}
