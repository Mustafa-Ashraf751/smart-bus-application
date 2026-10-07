package com.verysmartbus.mapper;

import com.verysmartbus.dto.response.MapPointResponseDto;
import com.verysmartbus.dto.request.RoutePathPointRequestDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class LocationMapper {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    public Point toPoint(BigDecimal latitude, BigDecimal longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude.doubleValue(), latitude.doubleValue())
        );
        point.setSRID(WGS84_SRID);
        return point;
    }

    public MapPointResponseDto toResponseDto(Point point) {
        if (point == null) {
            return null;
        }
        return new MapPointResponseDto(
                BigDecimal.valueOf(point.getX()),
                BigDecimal.valueOf(point.getY())
        );
    }

    public LineString toLineString(List<RoutePathPointRequestDto> points) {
        Coordinate[] coordinates = points.stream()
                .map(point -> new Coordinate(
                        point.longitude().doubleValue(),
                        point.latitude().doubleValue()
                ))
                .toArray(Coordinate[]::new);

        LineString lineString = GEOMETRY_FACTORY.createLineString(coordinates);
        lineString.setSRID(WGS84_SRID);
        return lineString;
    }

    public List<MapPointResponseDto> toResponseDto(LineString lineString) {
        if (lineString == null) {
            return null;
        }

        return Arrays.stream(lineString.getCoordinates())
                .map(coordinate -> new MapPointResponseDto(
                        BigDecimal.valueOf(coordinate.getX()),
                        BigDecimal.valueOf(coordinate.getY())
                ))
                .toList();
    }
}
