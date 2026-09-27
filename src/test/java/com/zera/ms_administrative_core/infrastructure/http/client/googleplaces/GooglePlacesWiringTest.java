package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class GooglePlacesWiringTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @DisplayName("When the integration is disabled (the default)")
    class Disabled {

        @Autowired
        private RecyclingPlaceFinder recyclingPlaceFinder;

        @Test
        @DisplayName("Should register the disabled stub, never the real Google client")
        void shouldRegisterDisabledStub() {
            assertThat(recyclingPlaceFinder).isInstanceOf(DisabledRecyclingPlaceFinder.class);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "zera.places.enabled=true",
            "zera.places.api-key=test-key"
    })
    @DisplayName("When the integration is enabled")
    class Enabled {

        @Autowired
        private RecyclingPlaceFinder recyclingPlaceFinder;

        @Test
        @DisplayName("Should register the real Google Places client")
        void shouldRegisterGoogleClient() {
            assertThat(recyclingPlaceFinder).isInstanceOf(GooglePlacesClient.class);
        }
    }
}
