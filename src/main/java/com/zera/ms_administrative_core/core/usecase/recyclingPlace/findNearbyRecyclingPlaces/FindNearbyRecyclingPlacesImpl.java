package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FindNearbyRecyclingPlacesImpl implements FindNearbyRecyclingPlaces {

    private final RecyclingPlaceFinder recyclingPlaceFinder;
    private final RecyclingBusinessRepository recyclingBusinessRepository;
    private final int defaultRadiusMeters;
    private final int maxRadiusMeters;

    public FindNearbyRecyclingPlacesImpl(
            RecyclingPlaceFinder recyclingPlaceFinder,
            RecyclingBusinessRepository recyclingBusinessRepository,
            @Value("${zera.places.default-radius-meters:5000}") int defaultRadiusMeters,
            @Value("${zera.places.max-radius-meters:20000}") int maxRadiusMeters) {
        this.recyclingPlaceFinder = recyclingPlaceFinder;
        this.recyclingBusinessRepository = recyclingBusinessRepository;
        this.defaultRadiusMeters = defaultRadiusMeters;
        this.maxRadiusMeters = maxRadiusMeters;
    }

    @Override
    public List<RecyclingPlaceOutput> execute(double latitude, double longitude, Integer radiusMeters) {
        GeoCoordinate origin = new GeoCoordinate(latitude, longitude);
        int radius = resolveRadius(radiusMeters);

        List<RecyclingPlace> places = recyclingPlaceFinder.findNearby(origin, radius);

        // Um unico SELECT por busca: cruza os pins com as fichas ja vinculadas por place_id.
        Map<String, RecyclingBusiness> linkedByPlaceId = recyclingBusinessRepository
                .findByPlaceIdIn(places.stream().map(RecyclingPlace::placeId).toList()).stream()
                .collect(Collectors.toMap(RecyclingBusiness::getPlaceId, Function.identity()));

        return places.stream()
                .map(place -> RecyclingPlaceOutput.from(place, origin, linkedByPlaceId.get(place.placeId())))
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
