package com.verysmartbus.repository;

import com.verysmartbus.entity.TripStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripStationRepository extends JpaRepository<TripStation, Long> {

    List<TripStation> findAllByTrip_IdOrderByStopOrderAsc(Long tripId);

    boolean existsByTrip_IdAndStation_Id(Long tripId, Long stationId);

    boolean existsByTrip_IdAndStopOrder(Long tripId, Integer stopOrder);
    Optional<TripStation> findByTrip_IdAndStation_Id(Long tripId, Long stationId);

    long countByTrip_Id(Long tripId);

    @Modifying
    @Query("""
            update TripStation tripStation
            set tripStation.stopOrder = tripStation.stopOrder + :offset
            where tripStation.trip.id = :tripId and tripStation.stopOrder >= :stopOrder
            """)
    void moveStopOrdersToTemporaryRange(
            @Param("tripId") Long tripId,
            @Param("stopOrder") Integer stopOrder,
            @Param("offset") Integer offset
    );

    @Modifying
    @Query("""
            update TripStation tripStation
            set tripStation.stopOrder = tripStation.stopOrder - :offset + 1
            where tripStation.trip.id = :tripId and tripStation.stopOrder >= :temporaryStopOrder
            """)
    void moveTemporaryStopOrdersUp(
            @Param("tripId") Long tripId,
            @Param("temporaryStopOrder") Integer temporaryStopOrder,
            @Param("offset") Integer offset
    );

    @Modifying
    @Query("""
            update TripStation tripStation
            set tripStation.stopOrder = tripStation.stopOrder + :offset
            where tripStation.trip.id = :tripId and tripStation.stopOrder > :stopOrder
            """)
    void moveFollowingStopOrdersToTemporaryRange(
            @Param("tripId") Long tripId,
            @Param("stopOrder") Integer stopOrder,
            @Param("offset") Integer offset
    );

    @Modifying
    @Query("""
            update TripStation tripStation
            set tripStation.stopOrder = tripStation.stopOrder - :offset - 1
            where tripStation.trip.id = :tripId and tripStation.stopOrder > :temporaryStopOrder
            """)
    void moveTemporaryStopOrdersDown(
            @Param("tripId") Long tripId,
            @Param("temporaryStopOrder") Integer temporaryStopOrder,
            @Param("offset") Integer offset
    );
}
