package com.verysmartbus.service;

import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Today's trips are created lazily: whoever opens the app first in a day creates them for
 * everyone, so no driver, admin or scheduler is responsible for it.
 * It is safe to call on every request: it costs one query once everything exists.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyTripService {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final DailyTripCreator dailyTripCreator;
    private final TripMapper tripMapper;

    /** Deliberately not @Transactional: every route gets its own transaction in DailyTripCreator. */
    public void ensureTodayTrips() {
        LocalDate today = LocalDate.now(CAIRO);

        for (Long routeId : routeRepository.findActiveRouteIdsWithoutTripOn(today)) {
            try {
                dailyTripCreator.createIfMissing(routeId, today);
            } catch (DataIntegrityViolationException e) {
                // Another request created this route's trip a moment earlier (unique route + date).
                log.debug("Trip for route {} on {} was created concurrently.", routeId, today);
            } catch (RuntimeException e) {
                // Never fail the user's request because trip generation failed.
                log.error("Could not generate the trip for route {} on {}.", routeId, today, e);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TripResponseDto> findTodayTrips() {
        return tripRepository.findByServiceDateOrderByScheduledStartTimeAsc(LocalDate.now(CAIRO)).stream()
                .map(tripMapper::toResponseDto)
                .toList();
    }
}
