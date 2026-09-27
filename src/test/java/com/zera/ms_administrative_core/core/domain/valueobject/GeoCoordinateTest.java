package com.zera.ms_administrative_core.core.domain.valueobject;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCoordinateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoCoordinateTest {

    @Test
    @DisplayName("Should accept a plausible coordinate")
    void shouldAcceptValidCoordinate() {
        GeoCoordinate coordinate = new GeoCoordinate(-23.5505, -46.6333);

        assertEquals(-23.5505, coordinate.latitude());
        assertEquals(-46.6333, coordinate.longitude());
    }

    @Test
    @DisplayName("Should reject latitude out of range")
    void shouldRejectLatitudeOutOfRange() {
        assertThrows(InvalidCoordinateException.class, () -> new GeoCoordinate(90.1, 0));
        assertThrows(InvalidCoordinateException.class, () -> new GeoCoordinate(-90.1, 0));
    }

    @Test
    @DisplayName("Should reject longitude out of range")
    void shouldRejectLongitudeOutOfRange() {
        assertThrows(InvalidCoordinateException.class, () -> new GeoCoordinate(0, 180.1));
        assertThrows(InvalidCoordinateException.class, () -> new GeoCoordinate(0, -180.1));
    }

    @Test
    @DisplayName("Should accept boundary values")
    void shouldAcceptBoundaryValues() {
        new GeoCoordinate(90, 180);
        new GeoCoordinate(-90, -180);
    }

    @Test
    @DisplayName("Should compute the distance between two coordinates in Sao Paulo via haversine")
    void shouldComputeDistanceBetweenTwoPoints() {
        // Praca da Se e MASP, distancia real ~ 4.3km
        GeoCoordinate pracaDaSe = new GeoCoordinate(-23.5503, -46.6339);
        GeoCoordinate masp = new GeoCoordinate(-23.5614, -46.6558);

        long distance = pracaDaSe.distanceMetersTo(masp);

        assertTrue(distance > 2000 && distance < 3500, "expected ~2.7km, got " + distance);
    }

    @Test
    @DisplayName("Distance from a coordinate to itself should be zero")
    void shouldReturnZeroDistanceToItself() {
        GeoCoordinate coordinate = new GeoCoordinate(-23.5505, -46.6333);

        assertEquals(0, coordinate.distanceMetersTo(coordinate));
    }
}
