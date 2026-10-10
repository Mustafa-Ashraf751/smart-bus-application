package com.verysmartbus.service;

import com.verysmartbus.dto.request.TripStationRequestDto;
import com.verysmartbus.dto.request.TripStationArrivalTimeUpdateRequestDto;
import com.verysmartbus.dto.response.TripStationResponseDto;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.TripStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.TripStationMapper;
import com.verysmartbus.repository.StationRepository;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.TripStationRepository;
import com.verysmartbus.security.BusAdminTripAccessAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripStationService {

    private static final int STOP_ORDER_OFFSET = 1_000_000;

    private final TripStationRepository tripStationRepository;
    private final StationRepository stationRepository;
    private final ReservationRepository reservationRepository;
    private final BusAdminTripAccessAuthorizer accessAuthorizer;
    private final TripStationMapper mapper;

    public List<TripStationResponseDto> findAllByTripId(Long busAdminId, Long tripId) {
        accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        return tripStationRepository.findAllByTrip_IdOrderByStopOrderAsc(tripId).stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public TripStationResponseDto addStation(Long busAdminId, Long tripId, TripStationRequestDto dto) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripCanBeEdited(trip);

        if (tripStationRepository.existsByTrip_IdAndStation_Id(tripId, dto.stationId())) {
            throw new IllegalArgumentException("This station is already assigned to the trip");
        }
        long stationCount = tripStationRepository.countByTrip_Id(tripId);
        if (dto.stopOrder() > stationCount + 1) {
            throw new IllegalArgumentException("stopOrder cannot be greater than the next available stop order");
        }

        Station station = findStationById(dto.stationId());
        shiftStopOrdersUp(tripId, dto.stopOrder());

        TripStation saved = tripStationRepository.save(mapper.toEntity(dto, trip, station));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public TripStationResponseDto updateArrivalTime(
            Long busAdminId,
            Long tripId,
            Long tripStationId,
            TripStationArrivalTimeUpdateRequestDto dto
    ) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripCanBeEdited(trip);
        TripStation tripStation = findEntityForTrip(tripId, tripStationId);
        tripStation.setExpectedArrivalTime(dto.expectedArrivalTime());
        return mapper.toResponseDto(tripStation);
    }

    @Transactional
    public void delete(Long busAdminId, Long tripId, Long tripStationId) {
        Trip trip = accessAuthorizer.findAssignedTrip(busAdminId, tripId);
        ensureTripCanBeEdited(trip);
        TripStation tripStation = findEntityForTrip(tripId, tripStationId);
        if (reservationRepository.existsByPickupTripStation_Id(tripStationId)) {
            throw new IllegalStateException("This station cannot be removed because it is used by a reservation.");
        }
        if (tripStationRepository.countByTrip_Id(tripId) == 1) {
            throw new IllegalStateException("A trip must keep at least one station.");
        }

        int deletedStopOrder = tripStation.getStopOrder();
        tripStationRepository.delete(tripStation);
        tripStationRepository.flush();
        shiftStopOrdersDown(tripId, deletedStopOrder);
    }

    private TripStation findEntityForTrip(Long tripId, Long tripStationId) {
        TripStation tripStation = tripStationRepository.findById(tripStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip station", tripStationId));
        if (!tripStation.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("The trip station does not belong to this trip.");
        }
        return tripStation;
    }

    private void shiftStopOrdersUp(Long tripId, Integer stopOrder) {
        tripStationRepository.moveStopOrdersToTemporaryRange(tripId, stopOrder, STOP_ORDER_OFFSET);
        tripStationRepository.moveTemporaryStopOrdersUp(tripId, stopOrder + STOP_ORDER_OFFSET, STOP_ORDER_OFFSET);
    }

    private void shiftStopOrdersDown(Long tripId, Integer deletedStopOrder) {
        tripStationRepository.moveFollowingStopOrdersToTemporaryRange(tripId, deletedStopOrder, STOP_ORDER_OFFSET);
        tripStationRepository.moveTemporaryStopOrdersDown(
                tripId, deletedStopOrder + STOP_ORDER_OFFSET, STOP_ORDER_OFFSET);
    }

    private void ensureTripCanBeEdited(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Trip stations can be changed only while a trip is scheduled.");
        }
    }

    private Station findStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station", stationId));
    }
}
