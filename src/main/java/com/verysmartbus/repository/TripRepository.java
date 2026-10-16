package com.verysmartbus.repository;

import com.verysmartbus.entity.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByRoute_Id(Long routeId);
    List<Trip> findAllByBusAdmin_IdOrderByServiceDateAscScheduledStartTimeAsc(Long busAdminId);
    boolean existsByRoute_IdAndServiceDate(Long routeId, LocalDate serviceDate);
    Optional<Trip> findFirstByRoute_IdAndScheduledStartTimeBetween(
            Long routeId, LocalDateTime start, LocalDateTime end);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
    List<Trip> findByServiceDateOrderByScheduledStartTimeAsc(LocalDate serviceDate);

    /** All trips relevant to one user: operated by them or reserved by them. */
    @Query("""
            select t
            from Trip t
            where t.driver.id = :userId
               or (t.busAdmin is not null and t.busAdmin.id = :userId)
               or exists (
                   select 1 from Reservation reservation
                   where reservation.trip = t
                     and reservation.user.id = :userId
                     and reservation.status <> com.verysmartbus.entity.enums.ReservationStatus.CANCELLED
               )
            order by t.serviceDate asc, t.scheduledStartTime asc
            """)
    List<Trip> findRelevantTripsForUser(@Param("userId") Long userId);

    @Query("""
    select t
    from Trip t
    join t.route r
    where t.status = com.verysmartbus.entity.enums.TripStatus.SCHEDULED
      and t.serviceDate >= :today
      and r.status = com.verysmartbus.entity.enums.RouteStatus.ACTIVE
    order by t.serviceDate asc, t.scheduledStartTime asc
    """)
    List<Trip> findBookableTrips(@Param("today") LocalDate today);

}
