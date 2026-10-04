package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.FindNearbyRecyclingPlaces;
import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.RecyclingPlaceOutput;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecyclingPlaceController.class)
class RecyclingPlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindNearbyRecyclingPlaces findNearbyRecyclingPlaces;

    @Test
    @DisplayName("GET /api/v1/recycling-places - should return places ordered by distance")
    void shouldFindNearby() throws Exception {
        RecyclingPlaceOutput place = new RecyclingPlaceOutput("place-1", "Cooperativa Recicla SP", "Rua X, 123", 1240, null, null);
        when(findNearbyRecyclingPlaces.execute(-23.5505, -46.6333, null)).thenReturn(List.of(place));

        mockMvc.perform(get("/api/v1/recycling-places")
                        .param("lat", "-23.5505")
                        .param("lng", "-46.6333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value("place-1"))
                .andExpect(jsonPath("$[0].name").value("Cooperativa Recicla SP"))
                .andExpect(jsonPath("$[0].distanceMeters").value(1240));
    }

    @Test
    @DisplayName("GET /api/v1/recycling-places - should forward the requested radius")
    void shouldForwardRadius() throws Exception {
        when(findNearbyRecyclingPlaces.execute(-23.5505, -46.6333, 1000)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/recycling-places")
                        .param("lat", "-23.5505")
                        .param("lng", "-46.6333")
                        .param("radiusMeters", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(findNearbyRecyclingPlaces).execute(-23.5505, -46.6333, 1000);
    }

    @Test
    @DisplayName("GET /api/v1/recycling-places - should require lat and lng")
    void shouldRequireCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/recycling-places"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/recycling-places - should return 503 when the integration is unavailable")
    void shouldReturn503WhenUnavailable() throws Exception {
        when(findNearbyRecyclingPlaces.execute(any(Double.class), any(Double.class), isNull()))
                .thenThrow(new RecyclingPlacesUnavailableException("Busca de recicladoras proximas esta desativada nesta instancia"));

        mockMvc.perform(get("/api/v1/recycling-places")
                        .param("lat", "-23.5505")
                        .param("lng", "-46.6333"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Busca de recicladoras proximas esta desativada nesta instancia"));
    }
}
