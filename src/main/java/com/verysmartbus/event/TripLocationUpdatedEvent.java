package com.verysmartbus.event;

import com.verysmartbus.dto.response.TripLocationResponseDto;

public record TripLocationUpdatedEvent(TripLocationResponseDto location) {
}
