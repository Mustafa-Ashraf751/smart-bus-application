package com.verysmartbus.controller;

import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.service.DailyTripService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TodayTripControllerTest {

    @Test
    void ensuresDailyTripsBeforeReturningTheList() {
        DailyTripService dailyTripService = mock(DailyTripService.class);
        TodayTripController controller = new TodayTripController(dailyTripService);
        List<TripResponseDto> expected = List.of(mock(TripResponseDto.class));
        when(dailyTripService.findTodayTrips()).thenReturn(expected);

        var response = controller.today();

        assertEquals(expected, response.getBody());
        assertEquals(200, response.getStatusCode().value());
        var ordered = inOrder(dailyTripService);
        ordered.verify(dailyTripService).ensureTodayTrips();
        ordered.verify(dailyTripService).findTodayTrips();
    }
}
