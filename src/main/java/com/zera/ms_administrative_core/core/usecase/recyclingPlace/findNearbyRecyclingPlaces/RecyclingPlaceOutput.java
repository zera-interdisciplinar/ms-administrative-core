package com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;

import java.util.List;
import java.util.UUID;

/**
 * {@code recyclingBusinessId} e {@code email} vem preenchido quando o ponto ja foi vinculado a uma
 * ficha interna (ver {@code PATCH /api/v1/recyclings/{id}/place-id}); nulos para pontos so do Google.
 * {@code isOpen}, {@code description} e {@code openingHours} seguem o que o Google devolveu; nulos ou
 * vazios quando ele nao tem o dado.
 */
public record RecyclingPlaceOutput(String placeId, String name, String address, long distanceMeters,
                                   Boolean isOpen, String description, List<OpeningHoursOutput> openingHours,
                                   UUID recyclingBusinessId, String email) {

    public static RecyclingPlaceOutput from(RecyclingPlace place, GeoCoordinate origin, RecyclingBusiness linked) {
        return new RecyclingPlaceOutput(
                place.placeId(),
                place.name(),
                place.address(),
                origin.distanceMetersTo(place.location()),
                place.openNow(),
                place.description(),
                place.weekdayHours().stream().map(OpeningHoursOutput::from).toList(),
                linked != null ? linked.getId() : null,
                linked != null ? linked.getEmail().value() : null);
    }

    /** Uma linha de horario separada em dias e faixa: "Monday: 8:00 AM - 6:00 PM" vira ("Monday", "8:00 AM - 6:00 PM"). */
    public record OpeningHoursOutput(String days, String hours) {

        static OpeningHoursOutput from(String weekdayDescription) {
            int separator = weekdayDescription.indexOf(": ");
            if (separator < 0) {
                return new OpeningHoursOutput(weekdayDescription, "");
            }
            return new OpeningHoursOutput(weekdayDescription.substring(0, separator),
                    weekdayDescription.substring(separator + 2));
        }
    }
}
