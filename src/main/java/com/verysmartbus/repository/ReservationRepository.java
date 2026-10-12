package com.verysmartbus.repository;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByPickupTripStation_Id(Long tripStationId);

    boolean existsByDropoffTripStation_Id(Long tripStationId);

    List<Reservation> findByUser_Id(Long userId);

    List<Reservation> findByTrip_Id(Long tripId);

    Optional<Reservation> findByUser_IdAndTrip_Id(Long userId, Long tripId);

    boolean existsByUser_IdAndTrip_Id(Long userId, Long tripId);

    boolean existsByUser_IdAndTrip_IdAndStatus(Long userId, Long tripId, ReservationStatus status);

    long countByTrip_IdAndStatusNot(Long tripId, ReservationStatus status);

    // ---- used by the booking / waitlist logic ----

    /** Seats actually taken: only RESERVED counts, WAITLISTED never does. */
    long countByTrip_IdAndStatus(Long tripId, ReservationStatus status);

    /** Is there a live (not cancelled) reservation for this user on this trip? */
    boolean existsByUser_IdAndTrip_IdAndStatusNot(Long userId, Long tripId, ReservationStatus status);

    /** Oldest waiting passenger first; reservation id breaks ties between identical timestamps. */
    Optional<Reservation> findFirstByTrip_IdAndStatusOrderByReservedAtAscReservationIdAsc(
            Long tripId, ReservationStatus status);

    List<Reservation> findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(
            Long tripId, ReservationStatus status);
}
