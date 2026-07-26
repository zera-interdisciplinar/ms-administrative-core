package com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterRecyclingImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private RegisterRecyclingImpl registerRecycling;

    @Test
    @DisplayName("Should register recycling business successfully")
    void shouldRegisterSuccessfully() {
        String name = "Test Company";
        String cnpj = "11.222.333/0001-81";
        String email = "test@company.com";

        when(repository.save(any(RecyclingBusiness.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecyclingBusiness result = registerRecycling.execute(name, cnpj, email);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(name, result.getName());
        assertEquals(cnpj.replaceAll("\\D", ""), result.getCnpj().value());
        assertEquals(email, result.getEmail().value());

        verify(repository).save(any(RecyclingBusiness.class));
    }
}
