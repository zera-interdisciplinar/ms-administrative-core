package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecyclingPlaceTest {

    private static final GeoCoordinate LOCATION = new GeoCoordinate(-23.5510, -46.6335);

    @Test
    @DisplayName("Should leave the optional Google fields empty when built from the basic constructor")
    void basicConstructorLeavesOptionalFieldsEmpty() {
        RecyclingPlace place = new RecyclingPlace("place", "Place", "addr", LOCATION);

        assertNull(place.openNow());
        assertNull(place.description());
        assertTrue(place.weekdayHours().isEmpty());
    }

    @Test
    @DisplayName("Should normalize null weekday hours to an empty list")
    void nullWeekdayHoursBecomeEmpty() {
        RecyclingPlace place = new RecyclingPlace("place", "Place", "addr", LOCATION, true, null, null);

        assertTrue(place.weekdayHours().isEmpty());
    }

    @Test
    @DisplayName("Should keep the weekday hours it was given")
    void keepsWeekdayHours() {
        RecyclingPlace place = new RecyclingPlace("place", "Place", "addr", LOCATION, true, null, List.of("Monday: Closed"));

        assertTrue(place.weekdayHours().contains("Monday: Closed"));
    }
}
