package com.zera.ms_administrative_core.core.domain.exception;

public class RecyclingPlaceAlreadyLinkedException extends RuntimeException {

    public RecyclingPlaceAlreadyLinkedException(String placeId) {
        super("Recycling place already linked to another recycling business: " + placeId);
    }
}
