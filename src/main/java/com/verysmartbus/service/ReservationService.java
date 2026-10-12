package com.verysmartbus.service;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.TripStationStatus;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.BusMapper;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final TripStationRepository tripStationRepository;
    private final UserRepository userRepository;
    private final ReservationMapper mapper;
    private final BusMapper busMapper;

    // ------------------------------------------------------------------ booking

    @Transactional
    public ReservationResponseDto create(Long currentUserId, ReservationRequestDto dto) {
        // Lock the trip row: concurrent bookings queue up here, so the last seat
        // can never be handed to two people.
        Trip trip = tripRepository.findByIdForUpdate(dto.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip", dto.tripId()));

        assertBookable(trip);

        TripStation pickup = loadStopOnTrip(dto.pickupTripStationId(), trip, "Pickup");
        TripStation dropoff = loadStopOnTrip(dto.dropoffTripStationId(), trip, "Drop-off");
        if (pickup.getStopOrder() >= dropoff.getStopOrder()) {
            throw new IllegalArgumentException("Pickup stop must come before the drop-off stop on the trip.");
        }

        if (reservationRepository.existsByUser_IdAndTrip_IdAndStatusNot(
                currentUserId, trip.getId(), ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("You already have a reservation for this trip.");
        }

        AppUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        // Only RESERVED consumes a seat; WAITLISTED never does.
        long seatsTaken = reservationRepository.countByTrip_IdAndStatus(trip.getId(), ReservationStatus.RESERVED);
        long capacity = trip.getBus().getCapacity();
        ReservationStatus status = seatsTaken < capacity
                ? ReservationStatus.RESERVED
                : ReservationStatus.WAITLISTED;

        Reservation saved = reservationRepository.save(
                mapper.toEntity(user, trip, pickup, dropoff, status, ReservationSource.MANUAL));
        return mapper.toResponseDto(saved);
    }

    // ------------------------------------------------------------ cancel + promote

    @Transactional
    public ReservationResponseDto cancel(Long currentUserId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
        assertOwnership(reservation, currentUserId);

        Long tripId = reservation.getTrip().getId();
        // Same lock as create(): a cancel and a new booking must not both claim the freed seat.
        tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        boolean freesASeat = reservation.getStatus() == ReservationStatus.RESERVED;
        reservation.cancel();   // throws if it is already cancelled

        if (freesASeat) {
            reservationRepository
                    .findFirstByTrip_IdAndStatusOrderByReservedAtAscReservationIdAsc(tripId, ReservationStatus.WAITLISTED)
                    .ifPresent(Reservation::promote);
        }

        return mapper.toResponseDto(reservation);
    }

    // -------------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getForUser(Long userId) {
        return reservationRepository.findByUser_Id(userId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    /** Passenger list: RESERVED and WAITLISTED (cancelled ones are left out), oldest first. */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getPassengers(Long currentUserId, Long tripId) {
        Trip trip = findTrip(tripId);
        if (!canManageTrip(currentUserId, trip)) {
            throw new ForbiddenOperationException("You are not allowed to view this trip's passengers.");
        }

        return reservationRepository
                .findByTrip_IdAndStatusNotOrderByReservedAtAscReservationIdAsc(tripId, ReservationStatus.CANCELLED)
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusResponseDto getBus(Long currentUserId, Long tripId) {
        Trip trip = findTrip(tripId);

        boolean hasReservation = reservationRepository.existsByUser_IdAndTrip_IdAndStatusNot(
                currentUserId, tripId, ReservationStatus.CANCELLED);
        if (!hasReservation && !canManageTrip(currentUserId, trip)) {
            throw new ForbiddenOperationException("You are not allowed to view this trip's bus.");
        }

        return busMapper.toResponseDto(trip.getBus());
    }

    // -------------------------------------------------------------------- helpers

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    private void assertBookable(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled trip can be booked.");
        }
        if (trip.getServiceDate().isBefore(LocalDate.now(CAIRO))) {
            throw new IllegalStateException("This trip is in the past.");
        }
    }

    private TripStation loadStopOnTrip(Long tripStationId, Trip trip, String label) {
        TripStation stop = tripStationRepository.findById(tripStationId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " trip station", tripStationId));

        if (!stop.getTrip().getId().equals(trip.getId())) {
            throw new IllegalArgumentException(label + " station does not belong to the selected trip.");
        }
        if (stop.getStatus() == TripStationStatus.SKIPPED) {
            throw new IllegalStateException(label + " station is skipped on this trip.");
        }
        return stop;
    }

    private void assertOwnership(Reservation reservation, Long currentUserId) {
        if (!reservation.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You do not own this reservation.");
        }
    }

    /** ADMIN, the driver assigned to the trip, or the bus admin assigned to the trip. */
    private boolean canManageTrip(Long userId, Trip trip) {
        if (trip.getDriver().getId().equals(userId)) {
            return true;
        }
        if (trip.getBusAdmin() != null && trip.getBusAdmin().getId().equals(userId)) {
            return true;
        }
        return isAdmin(userId);
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findByIdWithRoles(userId)
                .map(user -> user.getRoles() != null
                        && user.getRoles().stream().anyMatch(role -> ADMIN_ROLE_NAME.equals(role.getName())))
                .orElse(false);
    }
}
