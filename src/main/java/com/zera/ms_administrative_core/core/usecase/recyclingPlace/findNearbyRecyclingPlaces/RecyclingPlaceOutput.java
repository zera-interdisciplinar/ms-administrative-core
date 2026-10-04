package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import java.util.UUID;

/**
 * {@code recyclingBusinessId} e {@code email} vem preenchido quando o ponto ja foi vinculado a uma
 * ficha interna (ver {@code PATCH /api/v1/recyclings/{id}/place-id}); nulos para pontos so do Google.
 */
public record RecyclingPlaceOutput(String placeId, String name, String address, long distanceMeters,
                                   UUID recyclingBusinessId, String email) {

    public static RecyclingPlaceOutput from(RecyclingPlace place, GeoCoordinate origin, RecyclingBusiness linked) {
        return new RecyclingPlaceOutput(
                place.placeId(),
                place.name(),
                place.address(),
                origin.distanceMetersTo(place.location()),
                linked != null ? linked.getId() : null,
                linked != null ? linked.getEmail().value() : null);
    }
}
