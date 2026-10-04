package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesClientTest {

    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,"
            + "places.regularOpeningHours,places.currentOpeningHours,places.editorialSummary";
    private static final String NEARBY_URI = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String TEXT_URI = "https://places.googleapis.com/v1/places:searchText";
    private static final GeoCoordinate SAO_PAULO = new GeoCoordinate(-23.5505, -46.6333);

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://places.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private GooglePlacesClient client(int maxAttempts, Duration cacheTtl) {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        return new GooglePlacesClient(restClient, "test-key", maxAttempts, Duration.ofMillis(1),
                new PlacesCache(cacheTtl, clock));
    }

    private void expectEmptyTextSearches(String... termsAlreadyConsumed) {
        for (int i = termsAlreadyConsumed.length; i < 4; i++) {
            server.expect(requestTo(TEXT_URI))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withSuccess("{\"places\":[]}", MediaType.APPLICATION_JSON));
        }
    }

    @Test
    @DisplayName("Should send the minimal field mask on every call")
    void shouldSendFieldMaskHeader() {
        server.expect(requestTo(NEARBY_URI))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Goog-FieldMask", FIELD_MASK))
                .andExpect(header("X-Goog-Api-Key", "test-key"))
                .andRespond(withSuccess("{\"places\":[]}", MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000);

        server.verify();
    }

    @Test
    @DisplayName("Should map opening hours, open-now flag and editorial summary from the Places response")
    void shouldMapOpeningHoursAndDescription() {
        String body = "{\"places\":[{\"id\":\"place-1\",\"displayName\":{\"text\":\"Recicladora A\"},"
                + "\"formattedAddress\":\"Rua A, 1\",\"location\":{\"latitude\":-23.55,\"longitude\":-46.63},"
                + "\"currentOpeningHours\":{\"openNow\":true},"
                + "\"regularOpeningHours\":{\"weekdayDescriptions\":[\"Segunda-feira: 8:00 - 18:00\"]},"
                + "\"editorialSummary\":{\"text\":\"Recebe eletronicos.\"}}]}";
        server.expect(requestTo(NEARBY_URI)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        RecyclingPlace place = client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000).get(0);

        assertEquals(Boolean.TRUE, place.openNow());
        assertEquals("Recebe eletronicos.", place.description());
        assertEquals(List.of("Segunda-feira: 8:00 - 18:00"), place.weekdayHours());
        server.verify();
    }

    @Test
    @DisplayName("Should merge results from type search and text searches, deduplicating by placeId")
    void shouldMergeAndDeduplicateResults() {
        server.expect(requestTo(NEARBY_URI))
                .andRespond(withSuccess(placesJson(place("place-1", "Recicladora A", "Rua A, 1", -23.55, -46.63)),
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(TEXT_URI))
                .andRespond(withSuccess(placesJson(
                                place("place-1", "Recicladora A", "Rua A, 1", -23.55, -46.63),
                                place("place-2", "Ferro Velho B", "Rua B, 2", -23.56, -46.64)),
                        MediaType.APPLICATION_JSON));
        expectEmptyTextSearches("ferro velho");

        List<RecyclingPlace> result = client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000);

        assertEquals(2, result.size());
        server.verify();
    }

    @Test
    @DisplayName("Should retry with backoff and succeed after a transient failure")
    void shouldRetryAndSucceed() {
        server.expect(requestTo(NEARBY_URI)).andRespond(withServerError());
        server.expect(requestTo(NEARBY_URI))
                .andRespond(withSuccess("{\"places\":[]}", MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        List<RecyclingPlace> result = client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000);

        assertEquals(0, result.size());
        server.verify();
    }

    @Test
    @DisplayName("Should raise RecyclingPlacesUnavailableException after exhausting retries")
    void shouldFailAfterExhaustingRetries() {
        server.expect(requestTo(NEARBY_URI)).andRespond(withServerError());
        server.expect(requestTo(NEARBY_URI)).andRespond(withServerError());
        server.expect(requestTo(NEARBY_URI)).andRespond(withServerError());

        GooglePlacesClient client = client(3, Duration.ofHours(1));

        assertThrows(RecyclingPlacesUnavailableException.class, () -> client.findNearby(SAO_PAULO, 5000));
        server.verify();
    }

    @Test
    @DisplayName("Two calls with coordinates differing only in the 4th decimal should hit Google once")
    void shouldReuseCacheForNearbyCoordinates() {
        server.expect(requestTo(NEARBY_URI))
                .andRespond(withSuccess("{\"places\":[]}", MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        GooglePlacesClient client = client(3, Duration.ofHours(1));
        GeoCoordinate first = new GeoCoordinate(-23.55050, -46.63330);
        GeoCoordinate second = new GeoCoordinate(-23.55051, -46.63331);

        client.findNearby(first, 5000);
        client.findNearby(second, 5000);

        server.verify();
    }

    private static String place(String id, String name, String address, double lat, double lng) {
        return "{\"id\":\"" + id + "\",\"displayName\":{\"text\":\"" + name + "\"},"
                + "\"formattedAddress\":\"" + address + "\","
                + "\"location\":{\"latitude\":" + lat + ",\"longitude\":" + lng + "}}";
    }

    private static String placesJson(String... places) {
        return "{\"places\":[" + String.join(",", places) + "]}";
    }

    @Test
    @DisplayName("Should leave open-now, description and opening hours empty when Google omits them")
    void shouldLeaveOptionalFieldsEmptyWhenOmitted() {
        String body = "{\"places\":[{\"id\":\"place-9\",\"displayName\":{\"text\":\"Sem horario\"},"
                + "\"formattedAddress\":\"Rua X, 9\",\"location\":{\"latitude\":-23.55,\"longitude\":-46.63},"
                + "\"regularOpeningHours\":{}}]}";
        server.expect(requestTo(NEARBY_URI)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        RecyclingPlace place = client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000).get(0);

        assertEquals(null, place.openNow());
        assertEquals(null, place.description());
        assertTrue(place.weekdayHours().isEmpty());
    }

    @Test
    @DisplayName("Should treat a response without places as no results")
    void shouldTreatMissingPlacesAsEmpty() {
        server.expect(requestTo(NEARBY_URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        assertTrue(client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000).isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Should fall back to an empty name when Google omits the display name")
    void shouldFallBackToEmptyNameWithoutDisplayName() {
        String body = "{\"places\":[{\"id\":\"place-8\",\"formattedAddress\":\"Rua Z, 8\","
                + "\"location\":{\"latitude\":-23.55,\"longitude\":-46.63}}]}";
        server.expect(requestTo(NEARBY_URI)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        expectEmptyTextSearches();

        assertEquals("", client(3, Duration.ofHours(1)).findNearby(SAO_PAULO, 5000).get(0).name());
    }
}
