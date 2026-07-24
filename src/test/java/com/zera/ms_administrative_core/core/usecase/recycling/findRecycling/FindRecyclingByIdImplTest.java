package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindRecyclingByIdImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private FindRecyclingByIdImpl findRecyclingById;

    @Test
    @DisplayName("Should find recycling business by ID successfully")
    void shouldFindByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        RecyclingBusiness business = new RecyclingBusiness(id, "Test", new Cnpj("11.222.333/0001-81"), new Email("test@test.com"));
        when(repository.findById(id)).thenReturn(Optional.of(business));

        RecyclingBusiness result = findRecyclingById.execute(id);

        assertEquals(business, result);
    }

    @Test
    @DisplayName("Should throw exception when recycling business is not found by ID")
    void shouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> findRecyclingById.execute(id));
    }
}
