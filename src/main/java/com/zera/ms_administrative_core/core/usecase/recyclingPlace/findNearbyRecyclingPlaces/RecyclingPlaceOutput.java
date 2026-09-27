package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

public record RecyclingPlaceOutput(String placeId, String name, String address, long distanceMeters) {
    public static RecyclingPlaceOutput from(RecyclingPlace place, GeoCoordinate origin) {
        return new RecyclingPlaceOutput(
                place.placeId(),
                place.name(),
                place.address(),
                origin.distanceMetersTo(place.location()));
    }
}
