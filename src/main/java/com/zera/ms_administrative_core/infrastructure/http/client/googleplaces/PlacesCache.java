package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache em memoria, de curta duracao, apenas para conter o custo de chamadas repetidas ao Google
 * Places em pouco tempo (ex.: o usuario arrastando o mapa). Pelos Termos de Uso do Google, nome,
 * endereco e coordenada nao podem ser guardados por mais de 30 dias nem espelhados em banco -
 * por isso este cache vive so na memoria do processo, com TTL de poucas horas, e NUNCA deve
 * ganhar uma versao persistida em tabela.
 */
class PlacesCache {

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    PlacesCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    List<RecyclingPlace> get(GeoCoordinate center, int radiusMeters, Supplier<List<RecyclingPlace>> loader) {
        String key = keyFor(center, radiusMeters);
        Instant now = clock.instant();

        Entry cached = entries.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.places();
        }

        List<RecyclingPlace> loaded = loader.get();
        entries.put(key, new Entry(loaded, now.plus(ttl)));
        return loaded;
    }

    // Coordenadas arredondadas a 3 casas decimais (~110m): pequenos arrastos de mapa reaproveitam
    // a mesma entrada em vez de gerar uma chamada nova ao Google.
    private String keyFor(GeoCoordinate center, int radiusMeters) {
        return String.format(Locale.ROOT, "%.3f:%.3f:%d", center.latitude(), center.longitude(), radiusMeters);
    }

    private record Entry(List<RecyclingPlace> places, Instant expiresAt) {}
}
