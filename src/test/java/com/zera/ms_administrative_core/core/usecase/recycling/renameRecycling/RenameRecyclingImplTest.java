package com.zera.ms_administrative_core.core.usecase.recycling.renameRecycling;

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
class RenameRecyclingImplTest {

    @Mock
    private RecyclingBusinessRepository repository;

    @InjectMocks
    private RenameRecyclingImpl renameRecycling;

    private UUID id;
    private RecyclingBusiness business;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        business = new RecyclingBusiness(id, "Old Name", new Cnpj("11.222.333/0001-81"), new Email("test@company.com"));
    }

    @Test
    @DisplayName("Should rename successfully")
    void shouldRenameSuccessfully() {
        String newName = "New Name";
        when(repository.findById(id)).thenReturn(Optional.of(business));

        renameRecycling.execute(id, newName);

        assertEquals(newName, business.getName());
        verify(repository).save(business);
    }

    @Test
    @DisplayName("Should throw exception when recycling business is not found")
    void shouldThrowExceptionWhenNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> renameRecycling.execute(id, "New Name"));
        verify(repository, never()).save(any());
    }
}
