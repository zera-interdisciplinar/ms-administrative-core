package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class GooglePlacesConfig {

    private static final String BASE_URL = "https://places.googleapis.com";

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(name = "zera.places.enabled", havingValue = "true")
    RecyclingPlaceFinder googlePlacesFinder(
            Clock clock,
            @Value("${zera.places.api-key}") String apiKey,
            @Value("${zera.places.connect-timeout:5s}") Duration connectTimeout,
            @Value("${zera.places.read-timeout:5s}") Duration readTimeout,
            @Value("${zera.places.max-retries:3}") int maxRetries,
            @Value("${zera.places.retry-backoff:200ms}") Duration retryBackoff,
            @Value("${zera.places.cache-ttl:PT6H}") Duration cacheTtl) {

        // Timeouts explicitos: sem eles, um Google lento travaria a thread da requisicao alem
        // do aceitavel para um proxy fino.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());

        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();

        PlacesCache cache = new PlacesCache(cacheTtl, clock);
        return new GooglePlacesClient(restClient, apiKey, maxRetries, retryBackoff, cache);
    }

    @Bean
    @ConditionalOnProperty(name = "zera.places.enabled", havingValue = "false", matchIfMissing = true)
    RecyclingPlaceFinder disabledRecyclingPlaceFinder() {
        return new DisabledRecyclingPlaceFinder();
    }
}
