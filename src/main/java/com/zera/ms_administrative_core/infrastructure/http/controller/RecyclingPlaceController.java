package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.FindNearbyRecyclingPlaces;
import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.RecyclingPlaceOutput;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Proxy fino para o Google Places: nao ha catalogo local a cruzar, a resposta e o que o Google
 * devolve, limpa e ordenada por distancia. Distinto de {@code /api/v1/recyclings}, que e o
 * cadastro interno de empresas recicladoras (com CNPJ).
 */
@RestController
@RequestMapping("/api/v1/recycling-places")
public class RecyclingPlaceController {

    private final FindNearbyRecyclingPlaces findNearbyRecyclingPlaces;

    public RecyclingPlaceController(FindNearbyRecyclingPlaces findNearbyRecyclingPlaces) {
        this.findNearbyRecyclingPlaces = findNearbyRecyclingPlaces;
    }

    @GetMapping
    public ResponseEntity<List<RecyclingPlaceOutput>> findNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radiusMeters) {

        return ResponseEntity.ok(findNearbyRecyclingPlaces.execute(lat, lng, radiusMeters));
    }
}
