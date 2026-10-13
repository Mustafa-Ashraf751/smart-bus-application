package com.verysmartbus.service;

import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.entity.Trip;
import com.verysmartbus.mapper.TripMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyTripServiceTest {

    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final DailyTripCreator dailyTripCreator = mock(DailyTripCreator.class);
    private final TripMapper tripMapper = mock(TripMapper.class);
    private final DailyTripService service = new DailyTripService(routeRepository, tripRepository, dailyTripCreator, tripMapper);

    @Test
    void triesEveryActiveRouteMissingTodayAndLetsOneFailureNotBlockTheOthers() {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));
        when(routeRepository.findActiveRouteIdsWithoutTripOn(today)).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new DataIntegrityViolationException("another request won the race"))
                .when(dailyTripCreator).createIfMissing(2L, today);
        doThrow(new IllegalStateException("route data is incomplete"))
                .when(dailyTripCreator).createIfMissing(3L, today);

        service.ensureTodayTrips();

        verify(dailyTripCreator).createIfMissing(1L, today);
        verify(dailyTripCreator).createIfMissing(2L, today);
        verify(dailyTripCreator).createIfMissing(3L, today);
    }

    @Test
    void returnsTodayTripsInTheirScheduledOrderAsResponseDtos() {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));
        Trip first = Trip.builder().id(1L).build();
        Trip second = Trip.builder().id(2L).build();
        TripResponseDto firstResponse = mock(TripResponseDto.class);
        TripResponseDto secondResponse = mock(TripResponseDto.class);
        when(tripRepository.findByServiceDateOrderByScheduledStartTimeAsc(today)).thenReturn(List.of(first, second));
        when(tripMapper.toResponseDto(first)).thenReturn(firstResponse);
        when(tripMapper.toResponseDto(second)).thenReturn(secondResponse);

        List<TripResponseDto> result = service.findTodayTrips();

        assertEquals(List.of(firstResponse, secondResponse), result);
        verify(tripRepository).findByServiceDateOrderByScheduledStartTimeAsc(today);
    }
}
