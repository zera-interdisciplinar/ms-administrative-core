package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindRecyclingContactByPlaceIdImplTest {

    @Mock
    private RecyclingBusinessRepository recyclings;

    @Mock
    private TelephoneRepository telephones;

    @InjectMocks
    private FindRecyclingContactByPlaceIdImpl findContact;

    @Test
    @DisplayName("Should return email and phone when the place is linked")
    void shouldReturnContactWhenPlaceIsLinked() {
        UUID id = UUID.randomUUID();
        RecyclingBusiness business = business(id, "places/ChIJ");
        when(recyclings.findByPlaceId("places/ChIJ")).thenReturn(Optional.of(business));
        when(telephones.findByRecyclingBusinessId(id)).thenReturn(Optional.of(telephone(id)));

        RecyclingContact contact = findContact.execute(" places/ChIJ ");

        assertEquals(id, contact.recyclingBusinessId());
        assertEquals("Cooperativa", contact.name());
        assertEquals("contato@recicla.com", contact.email());
        assertEquals("11988887777", contact.phone());
    }

    @Test
    @DisplayName("Should return null phone when the recycling business has no telephone")
    void shouldReturnNullPhoneWhenTelephoneIsMissing() {
        UUID id = UUID.randomUUID();
        when(recyclings.findByPlaceId("places/ChIJ")).thenReturn(Optional.of(business(id, "places/ChIJ")));
        when(telephones.findByRecyclingBusinessId(id)).thenReturn(Optional.empty());

        RecyclingContact contact = findContact.execute("places/ChIJ");

        assertNull(contact.phone());
        assertEquals("contato@recicla.com", contact.email());
    }

    @Test
    @DisplayName("Should throw when no recycling business is linked to the place")
    void shouldThrowWhenPlaceIsNotLinked() {
        when(recyclings.findByPlaceId("places/missing")).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> findContact.execute("places/missing"));
    }

    @Test
    @DisplayName("Should reject a blank placeId")
    void shouldRejectBlankPlaceId() {
        assertThrows(IllegalArgumentException.class, () -> findContact.execute("  "));
    }

    private static RecyclingBusiness business(UUID id, String placeId) {
        return new RecyclingBusiness(id, "Cooperativa", new Cnpj("11.222.333/0001-81"),
                new Email("contato@recicla.com"), placeId, LocalDateTime.now(), LocalDateTime.now());
    }

    private static Telephone telephone(UUID recyclingBusinessId) {
        LocalDateTime now = LocalDateTime.now();
        return new Telephone(UUID.randomUUID(), new TelephoneNumber("11988887777"), null, null, null,
                recyclingBusinessId, now, now);
    }
}
