package com.zera.ms_administrative_core.core.domain.valueobject;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCoordinateException;

public record GeoCoordinate(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public GeoCoordinate {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidCoordinateException(latitude, longitude);
        }
    }

    public long distanceMetersTo(GeoCoordinate other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLng = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(EARTH_RADIUS_METERS * c);
    }
}
