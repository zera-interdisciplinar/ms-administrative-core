package com.zera.ms_administrative_core.core.domain.exception;

public class InvalidCoordinateException extends RuntimeException {
    public InvalidCoordinateException(double latitude, double longitude) {
        super("Invalid coordinate: lat=" + latitude + ", lng=" + longitude);
    }
}
