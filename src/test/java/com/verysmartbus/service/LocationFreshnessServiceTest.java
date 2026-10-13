package com.verysmartbus.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationFreshnessServiceTest {

    @Test
    void marksOnlyOldLocationsAsStale() {
        LocationFreshnessService service = new LocationFreshnessService(120);

        assertFalse(service.isStale(null));
        assertFalse(service.isStale(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(119)));
        assertTrue(service.isStale(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(121)));
    }

    @Test
    void rejectsNonPositiveStaleTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new LocationFreshnessService(0));
    }
}
