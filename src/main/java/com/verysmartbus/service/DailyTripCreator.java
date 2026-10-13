package com.verysmartbus.service;

import com.verysmartbus.entity.Reservation;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.TransportPreference;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.entity.TripStation;
import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;
import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.exception.ResourceNotFoundException;
import com.verysmartbus.mapper.ReservationMapper;
import com.verysmartbus.mapper.TripStationMapper;
import com.verysmartbus.repository.ReservationRepository;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.TransportPreferenceRepository;
import com.verysmartbus.repository.TripRepository;
import com.verysmartbus.repository.TripStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates one route's trip for one day: the Trip itself, its TripStations (copied from the
 * RouteStations) and a reservation for every active TransportPreference on that route.
 *
 * Each call runs in its own transaction, so a failure on one route never rolls back
 * the others. It lives in a separate bean so the transaction proxy actually applies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTripCreator {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final RouteStationRepository routeStationRepository;
    private final TripStationRepository tripStationRepository;
    private final TripStationMapper tripStationMapper;
    private final TransportPreferenceRepository preferenceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    /** @return true if a trip was created, false if it already existed or the route is not ready. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createIfMissing(Long routeId, LocalDate date) {
        if (tripRepository.existsByRoute_IdAndServiceDate(routeId, date)) {
            return false;
        }

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        if (route.getStatus() != RouteStatus.ACTIVE
                || route.getDefaultDepartureTime() == null
                || route.getDefaultBus() == null
                || route.getDefaultDriver() == null) {
            log.warn("Route {} is not ready for trip generation (inactive or missing defaults).", routeId);
            return false;
        }

        List<RouteStation> routeStations = routeStationRepository.findAllByRoute_IdOrderByStopOrderAsc(routeId);
        if (routeStations.isEmpty()) {
            log.warn("Route {} has no stations; no trip generated.", routeId);
            return false;
        }

        Trip trip = tripRepository.save(Trip.builder()
                .route(route)
                .bus(route.getDefaultBus())
                .driver(route.getDefaultDriver())
                .busAdmin(route.getDefaultBusAdmin())
                .serviceDate(date)
                .scheduledStartTime(date.atTime(route.getDefaultDepartureTime()).atZone(CAIRO).toOffsetDateTime())
                .build());

        List<TripStation> tripStations = tripStationRepository.saveAll(routeStations.stream()
                .map(routeStation -> tripStationMapper.fromRouteStation(routeStation, trip))
                .toList());

        int reservations = reserveForPreferences(trip, tripStations);
        log.info("Generated trip {} for route {} on {} with {} preference reservations.",
                trip.getId(), routeId, date, reservations);
        return true;
    }

    /**
     * Oldest preference first: the first ones get seats, the rest are WAITLISTED.
     * Reservation ids follow this order, which is also the waitlist promotion order.
     */
    private int reserveForPreferences(Trip trip, List<TripStation> tripStations) {
        Map<Long, TripStation> tripStationByStationId = tripStations.stream()
                .collect(Collectors.toMap(tripStation -> tripStation.getStation().getId(), Function.identity()));

        long capacity = trip.getBus().getCapacity();
        long seatsTaken = 0;
        List<Reservation> reservations = new ArrayList<>();

        List<TransportPreference> preferences = preferenceRepository
                .findByRoute_IdAndActiveTrueOrderByCreatedAtAscPreferenceIdAsc(trip.getRoute().getId());

        for (TransportPreference preference : preferences) {
            if (preference.getPickupRouteStation() == null || preference.getDropoffRouteStation() == null) {
                continue;   // legacy preference from before pickup/drop-off existed
            }

            TripStation pickup = tripStationByStationId.get(preference.getPickupRouteStation().getStation().getId());
            TripStation dropoff = tripStationByStationId.get(preference.getDropoffRouteStation().getStation().getId());
            if (pickup == null || dropoff == null || pickup.getStopOrder() >= dropoff.getStopOrder()) {
                log.warn("Preference {} does not fit trip {}; skipped.", preference.getPreferenceId(), trip.getId());
                continue;
            }

            ReservationStatus status = seatsTaken < capacity ? ReservationStatus.RESERVED : ReservationStatus.WAITLISTED;
            if (status == ReservationStatus.RESERVED) {
                seatsTaken++;
            }
            reservations.add(reservationMapper.toEntity(
                    preference.getUser(), trip, pickup, dropoff, status, ReservationSource.PREFERENCE));
        }

        reservationRepository.saveAll(reservations);
        return reservations.size();
    }
}
