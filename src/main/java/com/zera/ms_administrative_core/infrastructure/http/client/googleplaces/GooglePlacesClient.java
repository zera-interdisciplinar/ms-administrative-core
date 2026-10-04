package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.Circle;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.LatLng;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.LocationBias;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.LocationRestriction;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.PlaceDto;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.PlacesResponse;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.SearchNearbyRequest;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.SearchTextRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Adaptador da porta {@link RecyclingPlaceFinder} para a Places API (New) do Google. Concentra a
 * chave da API, o controle de custo (mascara de campos minima + cache curto) e a cobertura de
 * busca no Brasil (tipo + termos de texto), tudo isolado do dominio.
 */
class GooglePlacesClient implements RecyclingPlaceFinder {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);

    // Mascara explicita: cada campo pedido eleva o tier de cobranca da Places API (New). Horario e
    // descricao sobem o tier para Enterprise / Enterprise + Atmosphere e valem para TODAS as chamadas
    // (inclusive as de texto), porque a mascara e unica - acompanhar a cota no painel do Google.
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,"
            + "places.regularOpeningHours,places.currentOpeningHours,places.editorialSummary";

    // Sem languageCode o Google devolve os dias da semana em ingles ("Monday"); o app espera pt-BR.
    private static final String LANGUAGE_CODE = "pt-BR";

    private static final int MAX_RESULT_COUNT = 20;

    private static final String TYPE_RECYCLING_CENTER = "recycling_center";

    // includedTypes=recycling_center tem cobertura fraca no Brasil: boa parte dos locais esta
    // cadastrada como "ferro velho", "cooperativa de reciclagem", "ecoponto" ou "descarte
    // eletronico". Combinamos busca por tipo com searchText por esses termos e deduplicamos.
    private static final List<String> TEXT_SEARCH_TERMS = List.of(
            "ferro velho",
            "cooperativa de reciclagem",
            "ecoponto",
            "descarte eletronico");

    private final RestClient restClient;
    private final String apiKey;
    private final int maxAttempts;
    private final Duration retryBackoff;
    private final PlacesCache cache;

    GooglePlacesClient(RestClient restClient, String apiKey, int maxAttempts, Duration retryBackoff,
            PlacesCache cache) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = retryBackoff;
        this.cache = cache;
    }

    @Override
    public List<RecyclingPlace> findNearby(GeoCoordinate center, int radiusMeters) {
        return cache.get(center, radiusMeters, () -> fetchFromGoogle(center, radiusMeters));
    }

    private List<RecyclingPlace> fetchFromGoogle(GeoCoordinate center, int radiusMeters) {
        Map<String, RecyclingPlace> merged = new LinkedHashMap<>();

        callWithRetry(() -> searchByType(center, radiusMeters))
                .forEach(place -> merged.putIfAbsent(place.placeId(), place));

        for (String term : TEXT_SEARCH_TERMS) {
            callWithRetry(() -> searchByText(term, center, radiusMeters))
                    .forEach(place -> merged.putIfAbsent(place.placeId(), place));
        }

        return List.copyOf(merged.values());
    }

    private List<RecyclingPlace> searchByType(GeoCoordinate center, int radiusMeters) {
        SearchNearbyRequest body = new SearchNearbyRequest(
                List.of(TYPE_RECYCLING_CENTER),
                MAX_RESULT_COUNT,
                new LocationRestriction(new Circle(new LatLng(center.latitude(), center.longitude()), radiusMeters)),
                LANGUAGE_CODE);

        PlacesResponse response = restClient.post()
                .uri("/v1/places:searchNearby")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(body)
                .retrieve()
                .body(PlacesResponse.class);

        return toDomainList(response);
    }

    private List<RecyclingPlace> searchByText(String term, GeoCoordinate center, int radiusMeters) {
        SearchTextRequest body = new SearchTextRequest(
                term,
                new LocationBias(new Circle(new LatLng(center.latitude(), center.longitude()), radiusMeters)),
                MAX_RESULT_COUNT,
                LANGUAGE_CODE);

        PlacesResponse response = restClient.post()
                .uri("/v1/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(body)
                .retrieve()
                .body(PlacesResponse.class);

        return toDomainList(response);
    }

    private List<RecyclingPlace> toDomainList(PlacesResponse response) {
        if (response == null || response.places() == null) {
            return List.of();
        }
        return response.places().stream().map(this::toDomain).toList();
    }

    private RecyclingPlace toDomain(PlaceDto dto) {
        String name = dto.displayName() != null ? dto.displayName().text() : "";
        GeoCoordinate location = new GeoCoordinate(dto.location().latitude(), dto.location().longitude());
        Boolean openNow = dto.currentOpeningHours() != null ? dto.currentOpeningHours().openNow() : null;
        String description = dto.editorialSummary() != null ? dto.editorialSummary().text() : null;
        List<String> weekdayHours = dto.regularOpeningHours() != null
                && dto.regularOpeningHours().weekdayDescriptions() != null
                ? dto.regularOpeningHours().weekdayDescriptions()
                : List.of();
        return new RecyclingPlace(dto.id(), name, dto.formattedAddress(), location, openNow, description, weekdayHours);
    }

    // Backoff crescente, no maximo maxAttempts tentativas; depois disso, 503 em vez de lista
    // vazia, para o app distinguir "nao achei nada" de "nao consegui procurar".
    private <T> T callWithRetry(Supplier<T> operation) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("Chamada ao Google Places falhou (tentativa {}/{}): {}", attempt, maxAttempts,
                        ex.getMessage());
                if (attempt < maxAttempts) {
                    sleep(retryBackoff.multipliedBy(attempt));
                }
            }
        }

        throw new RecyclingPlacesUnavailableException(
                "Nao foi possivel buscar recicladoras proximas no momento", lastError);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
