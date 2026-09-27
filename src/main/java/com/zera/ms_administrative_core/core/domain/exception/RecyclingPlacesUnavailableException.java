package com.zera.ms_administrative_core.core.domain.exception;

public class RecyclingPlacesUnavailableException extends RuntimeException {
    public RecyclingPlacesUnavailableException(String message) {
        super(message);
    }

    public RecyclingPlacesUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
