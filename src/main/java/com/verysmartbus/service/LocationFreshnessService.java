package com.verysmartbus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class LocationFreshnessService {

    private final Duration staleAfter;

    public LocationFreshnessService(
            @Value("${app.live-location.stale-after-seconds}") long staleAfterSeconds
    ) {
        if (staleAfterSeconds <= 0) {
            throw new IllegalArgumentException("Live-location stale timeout must be positive.");
        }
        this.staleAfter = Duration.ofSeconds(staleAfterSeconds);
    }

    public boolean isStale(OffsetDateTime locationUpdatedAt) {
        return locationUpdatedAt != null
                && locationUpdatedAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minus(staleAfter));
    }
}
