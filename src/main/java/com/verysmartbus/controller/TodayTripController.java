package com.verysmartbus.controller;

import com.verysmartbus.dto.response.TripResponseDto;
import com.verysmartbus.service.DailyTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TodayTripController {

    private final DailyTripService dailyTripService;

    /** The app calls this on open: makes sure today's trips exist, then lists them. Any logged-in user. */
    @GetMapping("/today")
    public ResponseEntity<List<TripResponseDto>> today() {
        dailyTripService.ensureTodayTrips();
        return ResponseEntity.ok(dailyTripService.findTodayTrips());
    }
}
