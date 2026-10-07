package com.verysmartbus.repository;

import com.verysmartbus.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long>, StationSpatialRepository {

    @Query(value = """
            SELECT DISTINCT station.*
            FROM stations station
            JOIN trip_stations trip_station ON trip_station.station_id = station.id
            WHERE trip_station.trip_id = :tripId
              AND trip_station.status = 'ACTIVE'
              AND station.status = 'ACTIVE'
              AND extensions.ST_Covers(
                    station.area,
                    extensions.ST_SetSRID(
                            extensions.ST_MakePoint(:longitude, :latitude),
                            4326
                    )
              )
            ORDER BY station.id
            """, nativeQuery = true)
    List<Station> findActiveStationsForTripContaining(
            @Param("tripId") Long tripId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude
    );
}
