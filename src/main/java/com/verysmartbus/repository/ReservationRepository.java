package com.verysmartbus.repository;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUser_Id(Long userId);

    List<Reservation> findByTrip_Id(Long tripId);

    Optional<Reservation> findFirstByUser_Id(Long userId);
    Optional<Reservation> findByUser_IdAndTrip_Id(Long userId, Long tripId);

    boolean existsByUser_IdAndTrip_Id(Long userId, Long tripId);

    boolean existsByUser_IdAndTrip_IdAndStatus(Long userId, Long tripId, ReservationStatus status);

    long countByTrip_IdAndStatusNot(Long tripId, ReservationStatus status);
    Optional<Reservation> findByUser_IdAndStatusNot(Long userId, ReservationStatus status);}
