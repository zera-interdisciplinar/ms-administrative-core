package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllRecyclersImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private FindAllRecyclersImpl findAllRecyclers;

    @Test
    @DisplayName("Should return list of all recycling businesses")
    void shouldReturnAllRecyclers() {
        RecyclingBusiness b1 = new RecyclingBusiness(UUID.randomUUID(), "B1", new Cnpj("11.222.333/0001-81"), new Email("b1@test.com"));
        RecyclingBusiness b2 = new RecyclingBusiness(UUID.randomUUID(), "B2", new Cnpj("60.872.504/0001-23"), new Email("b2@test.com"));
        List<RecyclingBusiness> businesses = List.of(b1, b2);

        when(repository.findAll()).thenReturn(businesses);

        List<RecyclingBusiness> result = findAllRecyclers.execute();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(businesses));
    }

    @Test
    @DisplayName("Should return empty list when no recycling businesses exist")
    void shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<RecyclingBusiness> result = findAllRecyclers.execute();

        assertTrue(result.isEmpty());
    }
}
