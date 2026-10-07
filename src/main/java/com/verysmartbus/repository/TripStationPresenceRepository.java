package com.verysmartbus.repository;

import com.verysmartbus.entity.TripStationPresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripStationPresenceRepository extends JpaRepository<TripStationPresence, Long> {

    Optional<TripStationPresence> findByTrip_IdAndStation_Id(Long tripId, Long stationId);

    List<TripStationPresence> findAllByTrip_Id(Long tripId);
}
