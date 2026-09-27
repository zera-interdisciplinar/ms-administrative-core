package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class FindNearbyRecyclingPlacesImpl implements FindNearbyRecyclingPlaces {

    private final RecyclingPlaceFinder recyclingPlaceFinder;
    private final int defaultRadiusMeters;
    private final int maxRadiusMeters;

    public FindNearbyRecyclingPlacesImpl(
            RecyclingPlaceFinder recyclingPlaceFinder,
            @Value("${zera.places.default-radius-meters:5000}") int defaultRadiusMeters,
            @Value("${zera.places.max-radius-meters:20000}") int maxRadiusMeters) {
        this.recyclingPlaceFinder = recyclingPlaceFinder;
        this.defaultRadiusMeters = defaultRadiusMeters;
        this.maxRadiusMeters = maxRadiusMeters;
    }

    @Override
    public List<RecyclingPlaceOutput> execute(double latitude, double longitude, Integer radiusMeters) {
        GeoCoordinate origin = new GeoCoordinate(latitude, longitude);
        int radius = resolveRadius(radiusMeters);

        return recyclingPlaceFinder.findNearby(origin, radius).stream()
                .map(place -> RecyclingPlaceOutput.from(place, origin))
                .sorted(Comparator.comparingLong(RecyclingPlaceOutput::distanceMeters))
                .toList();
    }

    // Sem teto, uma tela com slider poderia pedir um raio de 500km e a conta do Google subiria
    // junto; por isso limitamos ao teto em vez de rejeitar a requisicao.
    private int resolveRadius(Integer requested) {
        if (requested == null) {
            return defaultRadiusMeters;
        }
        return Math.min(requested, maxRadiusMeters);
    }
}
