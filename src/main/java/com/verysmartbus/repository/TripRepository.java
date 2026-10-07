package com.verysmartbus.repository;

import com.verysmartbus.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByRoute_Id(Long routeId);
}
