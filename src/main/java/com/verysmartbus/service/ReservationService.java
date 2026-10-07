package com.verysmartbus.service;

import com.verysmartbus.dto.request.ReservationRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.dto.response.ReservationResponseDto;
import com.verysmartbus.entity.*;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.exception.ForbiddenOperationException;
import com.verysmartbus.mapper.BusMapper;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import com.verysmartbus.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final TripStationRepository tripStationRepository;
    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final ReservationMapper mapper;
    private final BusMapper busMapper;

    @Transactional
    public ReservationResponseDto create(Long currentUserId, ReservationRequestDto dto) {

        if (reservationRepository.existsByUser_IdAndTrip_Id(currentUserId, dto.tripId())) {
            throw new IllegalStateException("You already have a reservation for this trip.");
        }

        AppUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        Trip trip = tripRepository.findById(dto.tripId())
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + dto.tripId()));
        TripStation pickupTripStation = tripStationRepository.findById(dto.pickupTripStationId())
                .orElseThrow(() -> new EntityNotFoundException("Trip station not found: " + dto.pickupTripStationId()));


        if (!pickupTripStation.getTrip().getId().equals(trip.getId())) {
            throw new IllegalArgumentException("The chosen station does not belong to the selected trip.");
        }

// number of actual reservation
        long activeReservations = reservationRepository.countByTrip_IdAndStatusNot(
                trip.getId(), ReservationStatus.CANCELLED);

        Bus bus = busRepository.findById(trip.getId())
                .orElseThrow(() -> new EntityNotFoundException("Bus not found: " + trip.getId()));
        if (activeReservations >= bus.getCapacity()) {
            throw new IllegalStateException("This trip is fully booked.");
        }

        Reservation entity = mapper.toEntity(dto, user, trip, pickupTripStation);
        Reservation saved = reservationRepository.save(entity);

        return mapper.toResponseDto(saved);
    }

    @Transactional
    public ReservationResponseDto cancel(Long currentUserId, Long reservationId) {
        Reservation existing = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));
// btt2ked eno el owner el 7a2y2y
        assertOwnership(existing, currentUserId);
        existing.cancel();

        return mapper.toResponseDto(existing);
    }
// get all reservation for one user
    public List<ReservationResponseDto> getForUser(Long userId) {
        return reservationRepository.findByUser_Id(userId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }
// get all trip for one user
    public List<ReservationResponseDto> getForTrip(Long tripId) {
        return reservationRepository.findByTrip_Id(tripId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusResponseDto getBus(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + tripId));
        Bus bus = busRepository.findById(trip.getId())
                .orElseThrow(() -> new EntityNotFoundException("Bus not found for trip: " + tripId));
        return busMapper.toResponseDto(bus);
    }
// dy 3shan el malk el 7a2y2y hoa elly y2der yl3'y bas
    private void assertOwnership(Reservation reservation, Long currentUserId) {
        // ya3ny reservation da melk men -- get act user for this rev
        if (!reservation.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You do not own this reservation.");
        }
    }


}
