package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

public record RecyclingPlace(String placeId, String name, String address, GeoCoordinate location) {
}
