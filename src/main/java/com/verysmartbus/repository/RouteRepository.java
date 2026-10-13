package com.verysmartbus.repository;

import com.verysmartbus.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    @Query("""
            select r.id from Route r
            where r.status = com.verysmartbus.entity.enums.RouteStatus.ACTIVE
              and r.defaultDepartureTime is not null
              and r.defaultBus is not null
              and r.defaultDriver is not null
              and exists (select 1 from RouteStation rs where rs.route = r)
              and not exists (select 1 from Trip t where t.route = r and t.serviceDate = :date)
            """)
    List<Long> findActiveRouteIdsWithoutTripOn(@Param("date") LocalDate date);
}
