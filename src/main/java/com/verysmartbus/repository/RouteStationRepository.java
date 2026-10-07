package com.verysmartbus.repository;

import com.verysmartbus.entity.RouteStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStationRepository extends JpaRepository<RouteStation, Long> {

    List<RouteStation> findAllByRoute_IdOrderByStopOrderAsc(Long routeId);

    boolean existsByRoute_IdAndStation_Id(Long routeId, Long stationId);

    boolean existsByRoute_IdAndStopOrder(Long routeId, Integer stopOrder);
}
