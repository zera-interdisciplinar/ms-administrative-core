package com.zera.ms_administrative_core.core.usecase.recycling.linkRecyclingPlace;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlaceAlreadyLinkedException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkRecyclingPlaceImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private LinkRecyclingPlaceImpl usecase;

    private UUID id;
    private RecyclingBusiness business;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        business = new RecyclingBusiness(id, "Recicla SP", new Cnpj("11.222.333/0001-81"), new Email("a@b.com"));
    }

    @Test
    @DisplayName("Should link the place to the recycling business")
    void shouldLinkPlace() {
        when(repository.findById(id)).thenReturn(Optional.of(business));
        when(repository.findByPlaceIdIn(List.of("place-1"))).thenReturn(List.of());

        usecase.execute(id, "place-1");

        assertEquals("place-1", business.getPlaceId());
        verify(repository).save(business);
    }

    @Test
    @DisplayName("Should accept relinking the same place to the same business")
    void shouldAllowSameBusiness() {
        business.linkPlace("place-1");
        when(repository.findById(id)).thenReturn(Optional.of(business));
        when(repository.findByPlaceIdIn(List.of("place-1"))).thenReturn(List.of(business));

        usecase.execute(id, "place-1");

        verify(repository).save(business);
    }

    @Test
    @DisplayName("Should refuse a place already linked to another business")
    void shouldRefuseOtherBusiness() {
        RecyclingBusiness other = new RecyclingBusiness(UUID.randomUUID(), "Outra", new Cnpj("11.222.333/0001-81"),
                new Email("c@d.com"));
        when(repository.findById(id)).thenReturn(Optional.of(business));
        when(repository.findByPlaceIdIn(List.of("place-1"))).thenReturn(List.of(other));

        assertThrows(RecyclingPlaceAlreadyLinkedException.class, () -> usecase.execute(id, "place-1"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the recycling business does not exist")
    void shouldFailWhenNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> usecase.execute(id, "place-1"));
        verify(repository, never()).save(any());
    }
}
