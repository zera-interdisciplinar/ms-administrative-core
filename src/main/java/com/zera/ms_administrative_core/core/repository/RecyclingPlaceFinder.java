package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import java.util.List;

/**
 * Porta para busca de recicladoras proximas em um provedor externo de locais (hoje, Google
 * Places). O dominio nao conhece o provedor: se ele mudar, so a implementacao desta porta muda.
 */
public interface RecyclingPlaceFinder {
    List<RecyclingPlace> findNearby(GeoCoordinate center, int radiusMeters);
}
