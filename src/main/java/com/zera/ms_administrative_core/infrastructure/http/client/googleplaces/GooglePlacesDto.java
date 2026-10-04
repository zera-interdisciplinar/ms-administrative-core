package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import java.util.List;

/**
 * DTOs de request/response da Places API (New) - https://places.googleapis.com/v1/places:*.
 * Ficam isolados aqui, fora do dominio, porque sao um detalhe do provedor Google: o dominio
 * conhece apenas {@code RecyclingPlace} (via {@link com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder}).
 */
final class GooglePlacesDto {

    private GooglePlacesDto() {}

    record LatLng(double latitude, double longitude) {}

    record Circle(LatLng center, double radius) {}

    record LocationBias(Circle circle) {}

    record SearchTextRequest(String textQuery, LocationBias locationBias, int maxResultCount) {}

    record DisplayName(String text) {}

    record PlaceDto(String id, DisplayName displayName, String formattedAddress, LatLng location) {}

    record PlacesResponse(List<PlaceDto> places) {}
}
