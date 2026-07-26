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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindRecyclingByCnpjImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private FindRecyclingByCnpjImpl findRecyclingByCnpj;

    @Test
    @DisplayName("Should find recycling business by CNPJ successfully")
    void shouldFindByCnpjSuccessfully() {
        String cnpjStr = "11.222.333/0001-81";
        RecyclingBusiness business = new RecyclingBusiness(UUID.randomUUID(), "Test", new Cnpj(cnpjStr), new Email("test@test.com"));
        when(repository.findByCnpj(any(Cnpj.class))).thenReturn(Optional.of(business));

        RecyclingBusiness result = findRecyclingByCnpj.execute(cnpjStr);

        assertEquals(business, result);
    }

    @Test
    @DisplayName("Should throw exception when recycling business is not found by CNPJ")
    void shouldThrowExceptionWhenNotFound() {
        String cnpjStr = "11.222.333/0001-81";
        when(repository.findByCnpj(any(Cnpj.class))).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> findRecyclingByCnpj.execute(cnpjStr));
    }
}
