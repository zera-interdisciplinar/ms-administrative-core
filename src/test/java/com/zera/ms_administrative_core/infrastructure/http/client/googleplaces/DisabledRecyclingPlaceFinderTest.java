package com.zera.ms_administrative_core.infrastructure.http.client.googleplaces;

import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DisabledRecyclingPlaceFinderTest {

    @Test
    @DisplayName("Should always raise RecyclingPlacesUnavailableException, never an empty list")
    void shouldAlwaysThrow() {
        DisabledRecyclingPlaceFinder finder = new DisabledRecyclingPlaceFinder();

        assertThrows(RecyclingPlacesUnavailableException.class,
                () -> finder.findNearby(new GeoCoordinate(-23.5505, -46.6333), 5000));
    }
}
