package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;

import java.util.List;

/**
 * Adaptador padrao quando a integracao com o Google Places esta desligada
 * (zera.places.enabled=false, o padrao). Devolver lista vazia aqui seria enganoso - pareceria
 * "nenhuma recicladora perto de voce" quando na verdade a busca nem foi feita.
 */
class DisabledRecyclingPlaceFinder implements RecyclingPlaceFinder {

    @Override
    public List<RecyclingPlace> findNearby(GeoCoordinate center, int radiusMeters) {
        throw new RecyclingPlacesUnavailableException(
                "Busca de recicladoras proximas esta desativada nesta instancia");
    }
}
