package com.verysmartbus.repository;

import com.verysmartbus.entity.TripStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripStationRepository extends JpaRepository<TripStation, Long> {

    List<TripStation> findAllByTrip_IdOrderByStopOrderAsc(Long tripId);

    boolean existsByTrip_IdAndStation_Id(Long tripId, Long stationId);

    boolean existsByTrip_IdAndStopOrder(Long tripId, Integer stopOrder);
    Optional<TripStation> findByTrip_IdAndStation_Id(Long tripId, Long stationId);
}
