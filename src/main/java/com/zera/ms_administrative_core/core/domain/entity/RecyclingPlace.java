package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import java.util.List;

/**
 * Ponto descoberto no Google Places. {@code openNow}, {@code description} e {@code weekdayHours} so
 * vem preenchidos quando o Google os devolve (ver a mascara em GooglePlacesClient); sao efemeros e
 * vivem apenas no cache de memoria.
 */
public record RecyclingPlace(String placeId, String name, String address, GeoCoordinate location,
                             Boolean openNow, String description, List<String> weekdayHours) {

    public RecyclingPlace {
        weekdayHours = weekdayHours == null ? List.of() : List.copyOf(weekdayHours);
    }

    public RecyclingPlace(String placeId, String name, String address, GeoCoordinate location) {
        this(placeId, name, address, location, null, null, List.of());
    }
}
