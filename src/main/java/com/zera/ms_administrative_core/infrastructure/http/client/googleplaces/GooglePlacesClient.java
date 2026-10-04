package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.Circle;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.LatLng;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.LocationBias;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.PlaceDto;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.PlacesResponse;
import com.zera.ms_administrative_core.infrastructure.http.client.googleplaces.GooglePlacesDto.SearchTextRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
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
 * busca no Brasil (termos de texto), tudo isolado do dominio.
 */
class GooglePlacesClient implements RecyclingPlaceFinder {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);

    // Mascara minima e explicita: cada campo pedido eleva o tier de cobranca da Places API (New).
    // A tela so usa id, nome, endereco e coordenada - nada mais deve entrar aqui.
    // Mascara explicita: cada campo pedido eleva o tier de cobranca da Places API (New). Horario e
    // descricao sobem o tier para Enterprise / Enterprise + Atmosphere e valem para TODAS as chamadas
    // (inclusive as de texto), porque a mascara e unica - acompanhar a cota no painel do Google.
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,"
            + "places.regularOpeningHours,places.currentOpeningHours,places.editorialSummary";

    // Sem languageCode o Google devolve os dias da semana em ingles ("Monday"); o app espera pt-BR.
    private static final String LANGUAGE_CODE = "pt-BR";

    private static final int MAX_RESULT_COUNT = 20;

    // Nao usamos searchNearby com includedTypes: "recycling_center" nao e um tipo aceito pela
    // Places API (New) e a chamada inteira volta 400. Os locais de reciclagem no Brasil estao
    // cadastrados sob nomes populares, entao a busca e feita por esses termos e deduplicada.
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

        for (String term : TEXT_SEARCH_TERMS) {
            callWithRetry(() -> searchByText(term, center, radiusMeters))
                    .forEach(place -> merged.putIfAbsent(place.placeId(), place));
        }

        return List.copyOf(merged.values());
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
                log.warn("Chamada ao Google Places falhou (tentativa {}/{}): {}", attempt, maxAttempts,
                        ex.getMessage());
                // 4xx (exceto 429) significa requisicao invalida: repetir devolve o mesmo erro e so
                // atrasa a resposta ao usuario.
                if (!isRetryable(ex)) {
                    throw new RecyclingPlacesUnavailableException(
                            "Nao foi possivel buscar recicladoras proximas no momento", ex);
                }
                lastError = ex;
                if (attempt < maxAttempts) {
                    sleep(retryBackoff.multipliedBy(attempt));
                }
            }
        }

        throw new RecyclingPlacesUnavailableException(
                "Nao foi possivel buscar recicladoras proximas no momento", lastError);
    }

    private static boolean isRetryable(RestClientException ex) {
        return !(ex instanceof HttpClientErrorException client)
                || client.getStatusCode().value() == 429;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
