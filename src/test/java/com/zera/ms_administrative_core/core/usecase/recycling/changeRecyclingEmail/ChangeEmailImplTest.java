package com.zera.ms_administrative_core.core.usecase.recycling.changeRecyclingEmail;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeEmailImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private ChangeEmailImpl changeEmail;

    private UUID id;
    private RecyclingBusiness business;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        business = new RecyclingBusiness(id, "Test Company", new Cnpj("11.222.333/0001-81"), new Email("old@email.com"));
    }

    @Test
    @DisplayName("Should change email successfully")
    void shouldChangeEmailSuccessfully() {
        String newEmailStr = "new@email.com";
        when(repository.findById(id)).thenReturn(Optional.of(business));

        changeEmail.execute(id, newEmailStr);

        assertEquals(newEmailStr, business.getEmail().value());
        verify(repository).save(business);
    }

    @Test
    @DisplayName("Should throw exception when recycling business is not found")
    void shouldThrowExceptionWhenNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> changeEmail.execute(id, "new@email.com"));
        verify(repository, never()).save(any());
    }
}
