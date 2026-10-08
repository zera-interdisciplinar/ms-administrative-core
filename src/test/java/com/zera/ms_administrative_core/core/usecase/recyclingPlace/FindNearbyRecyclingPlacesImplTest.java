package com.zera.ms_administrative_core.core.usecase.recyclingPlace;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingPlace;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCoordinateException;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlacesUnavailableException;
import com.zera.ms_administrative_core.core.domain.valueobject.GeoCoordinate;
import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.RecyclingPlaceFinder;
import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.FindNearbyRecyclingPlacesImpl;
import com.zera.ms_administrative_core.core.usecase.recyclingPlace.findNearbyRecyclingPlaces.RecyclingPlaceOutput;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindNearbyRecyclingPlacesImplTest {

    private static final int DEFAULT_RADIUS = 5000;
    private static final int MAX_RADIUS = 20000;

    @Mock
    private RecyclingPlaceFinder recyclingPlaceFinder;

    @Mock
    private RecyclingBusinessRepository recyclingBusinessRepository;

    private FindNearbyRecyclingPlacesImpl usecase;

    @BeforeEach
    void setUp() {
        usecase = new FindNearbyRecyclingPlacesImpl(recyclingPlaceFinder, recyclingBusinessRepository, DEFAULT_RADIUS, MAX_RADIUS);
    }

    @Test
    @DisplayName("Should use the default radius when none is given")
    void shouldUseDefaultRadiusWhenNull() {
        when(recyclingPlaceFinder.findNearby(any(), eq(DEFAULT_RADIUS))).thenReturn(List.of());

        usecase.execute(-23.5505, -46.6333, null);

        verify(recyclingPlaceFinder).findNearby(any(), eq(DEFAULT_RADIUS));
    }

    @Test
    @DisplayName("Should clamp the radius to the ceiling instead of erroring out")
    void shouldClampRadiusToCeiling() {
        when(recyclingPlaceFinder.findNearby(any(), eq(MAX_RADIUS))).thenReturn(List.of());

        usecase.execute(-23.5505, -46.6333, 500_000);

        verify(recyclingPlaceFinder).findNearby(any(), eq(MAX_RADIUS));
    }

    @Test
    @DisplayName("Should keep a radius that is already within bounds")
    void shouldKeepRadiusWithinBounds() {
        when(recyclingPlaceFinder.findNearby(any(), eq(1000))).thenReturn(List.of());

        usecase.execute(-23.5505, -46.6333, 1000);

        verify(recyclingPlaceFinder).findNearby(any(), eq(1000));
    }

    @Test
    @DisplayName("Should reject an implausible coordinate before calling the port")
    void shouldRejectInvalidCoordinate() {
        assertThrows(InvalidCoordinateException.class, () -> usecase.execute(200, 0, null));
    }

    @Test
    @DisplayName("Should return results ordered by ascending distance")
    void shouldSortResultsByDistance() {
        GeoCoordinate origin = new GeoCoordinate(-23.5505, -46.6333);
        RecyclingPlace far = new RecyclingPlace("far", "Far", "addr", new GeoCoordinate(-23.6000, -46.7000));
        RecyclingPlace near = new RecyclingPlace("near", "Near", "addr", new GeoCoordinate(-23.5510, -46.6335));
        when(recyclingPlaceFinder.findNearby(any(), anyInt())).thenReturn(List.of(far, near));

        List<RecyclingPlaceOutput> result = usecase.execute(origin.latitude(), origin.longitude(), null);

        assertEquals(2, result.size());
        assertEquals("near", result.get(0).placeId());
        assertEquals(-23.5510, result.get(0).lat());
        assertEquals(-46.6335, result.get(0).lng());
        assertEquals("far", result.get(1).placeId());
        assertTrue(result.get(0).distanceMeters() < result.get(1).distanceMeters());
    }

    @Test
    @DisplayName("Should propagate the unavailable exception from the port")
    void shouldPropagateUnavailableException() {
        when(recyclingPlaceFinder.findNearby(any(), anyInt()))
                .thenThrow(new RecyclingPlacesUnavailableException("indisponivel"));

        assertThrows(RecyclingPlacesUnavailableException.class,
                () -> usecase.execute(-23.5505, -46.6333, null));
    }

    @Test
    @DisplayName("Should return an empty list when Google succeeds without results in range")
    void shouldReturnEmptyListWhenNoResults() {
        when(recyclingPlaceFinder.findNearby(any(), anyInt())).thenReturn(List.of());

        List<RecyclingPlaceOutput> result = usecase.execute(-23.5505, -46.6333, null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should expose the linked recycling business when the place is linked")
    void shouldExposeLinkedRecyclingBusiness() {
        UUID businessId = UUID.randomUUID();
        RecyclingBusiness business = new RecyclingBusiness(businessId, "Recicla SP",
                new Cnpj("11.222.333/0001-81"), new Email("contato@recicla.com"));
        business.linkPlace("linked");
        RecyclingPlace linked = new RecyclingPlace("linked", "Linked", "addr", new GeoCoordinate(-23.5510, -46.6335));
        RecyclingPlace unlinked = new RecyclingPlace("unlinked", "Unlinked", "addr", new GeoCoordinate(-23.5520, -46.6340));
        when(recyclingPlaceFinder.findNearby(any(), anyInt())).thenReturn(List.of(linked, unlinked));
        when(recyclingBusinessRepository.findByPlaceIdIn(List.of("linked", "unlinked"))).thenReturn(List.of(business));

        List<RecyclingPlaceOutput> result = usecase.execute(-23.5505, -46.6333, null);

        RecyclingPlaceOutput withBusiness = result.stream().filter(p -> p.placeId().equals("linked")).findFirst().orElseThrow();
        RecyclingPlaceOutput withoutBusiness = result.stream().filter(p -> p.placeId().equals("unlinked")).findFirst().orElseThrow();
        assertEquals(businessId, withBusiness.recyclingBusinessId());
        assertEquals("contato@recicla.com", withBusiness.email());
        assertEquals(null, withoutBusiness.recyclingBusinessId());
        assertEquals(null, withoutBusiness.email());
    }

    @Test
    @DisplayName("Should split each weekday line into days and hours, keeping lines without a separator whole")
    void shouldSplitOpeningHours() {
        RecyclingPlace place = new RecyclingPlace("place", "Place", "addr", new GeoCoordinate(-23.5510, -46.6335),
                true, "Recebe eletronicos.", List.of("Monday: 8:00 AM - 6:00 PM", "Sunday"));
        when(recyclingPlaceFinder.findNearby(any(), anyInt())).thenReturn(List.of(place));

        RecyclingPlaceOutput output = usecase.execute(-23.5505, -46.6333, null).get(0);

        assertEquals(Boolean.TRUE, output.isOpen());
        assertEquals("Recebe eletronicos.", output.description());
        assertEquals(new RecyclingPlaceOutput.OpeningHoursOutput("Monday", "8:00 AM - 6:00 PM"), output.openingHours().get(0));
        assertEquals(new RecyclingPlaceOutput.OpeningHoursOutput("Sunday", ""), output.openingHours().get(1));
    }
}
