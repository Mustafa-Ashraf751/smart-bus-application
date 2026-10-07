package com.verysmartbus.repository;

import com.verysmartbus.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByRoute_Id(Long routeId);
    boolean existsByRoute_IdAndServiceDate(Long routeId, LocalDate serviceDate);
    Optional<Trip> findFirstByRoute_IdAndScheduledStartTimeBetween(
            Long routeId, LocalDateTime start, LocalDateTime end);

}
