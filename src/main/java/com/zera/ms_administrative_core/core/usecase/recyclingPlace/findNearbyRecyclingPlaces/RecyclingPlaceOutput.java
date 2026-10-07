package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

public record RecyclingPlaceOutput(
        String placeId, String name, String address, double lat, double lng, long distanceMeters) {
    public static RecyclingPlaceOutput from(RecyclingPlace place, GeoCoordinate origin) {
        GeoCoordinate location = place.location();
        return new RecyclingPlaceOutput(
                place.placeId(),
                place.name(),
                place.address(),
                location.latitude(),
                location.longitude(),
                origin.distanceMetersTo(location));
    }
}
