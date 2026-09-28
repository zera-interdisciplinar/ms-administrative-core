package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import java.util.List;

public interface FindNearbyRecyclingPlaces {
    List<RecyclingPlaceOutput> execute(double latitude, double longitude, Integer radiusMeters);
}
