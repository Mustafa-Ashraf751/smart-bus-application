package com.verysmartbus.repository;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByPickupTripStation_Id(Long tripStationId);

    List<Reservation> findByUser_Id(Long userId);


    boolean existsByUser_IdAndTrip_IdAndStatus(Long userId, Long tripId, ReservationStatus status);

    long countByTrip_IdAndStatus(Long tripId, ReservationStatus status);
    boolean existsByUser_IdAndTrip_IdAndStatusNot(Long userId, Long tripId, ReservationStatus status);

    Optional<Reservation> findFirstByTrip_IdAndStatusOrderByReservedAtAscReservationIdAsc(
            Long tripId, ReservationStatus status);

    List<Reservation> findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
            Long tripId, ReservationStatus status);

    /** All live (not cancelled) reservations of several trips at once, for the available-trips list. */
    List<Reservation> findByTrip_IdInAndStatusNot(Collection<Long> tripIds, ReservationStatus status);
}
