package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.RoutePathPointRequestDto;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationMapperTest {

    private final LocationMapper mapper = new LocationMapper();

    @Test
    void convertsLatitudeLongitudeToWgs84PointAndBack() {
        Point point = mapper.toPoint(new BigDecimal("30.0444"), new BigDecimal("31.2357"));

        assertEquals(4326, point.getSRID());
        assertEquals(31.2357, point.getX(), 0.00001);
        assertEquals(30.0444, point.getY(), 0.00001);
        assertEquals(new BigDecimal("31.2357"), mapper.toResponseDto(point).longitude());
        assertEquals(new BigDecimal("30.0444"), mapper.toResponseDto(point).latitude());
        assertNull(mapper.toResponseDto((Point) null));
    }

    @Test
    void convertsRoutePathPointsToLineString() {
        var line = mapper.toLineString(List.of(
                new RoutePathPointRequestDto(new BigDecimal("30.0"), new BigDecimal("31.0")),
                new RoutePathPointRequestDto(new BigDecimal("30.1"), new BigDecimal("31.1"))
        ));

        assertEquals(4326, line.getSRID());
        assertEquals(2, line.getNumPoints());
    }
}
