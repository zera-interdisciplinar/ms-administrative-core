package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RecyclingBusinessJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.RecyclingMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecyclingJpaRepositoryImplTest {

    @Mock
    private RecyclingJpaRepository jpa;

    @Mock
    private RecyclingMapper mapper;

    @InjectMocks
    private RecyclingBusinessRepositoryImpl repository;

    @Test
    @DisplayName("Should save recycling business")
    void shouldSave() {
        RecyclingBusiness domain = new RecyclingBusiness(UUID.randomUUID(), "Test", new Cnpj("11.222.333/0001-81"), new Email("test@test.com"));
        RecyclingBusinessJpa entity = mock(RecyclingBusinessJpa.class);
        RecyclingBusinessJpa savedEntity = mock(RecyclingBusinessJpa.class);

        when(mapper.toJpa(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(domain);

        RecyclingBusiness result = repository.save(domain);

        assertEquals(domain, result);
        verify(jpa).save(entity);
    }

    @Test
    @DisplayName("Should find by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        RecyclingBusinessJpa entity = mock(RecyclingBusinessJpa.class);
        RecyclingBusiness domain = mock(RecyclingBusiness.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<RecyclingBusiness> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    @DisplayName("Should find by CNPJ")
    void shouldFindByCnpj() {
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");
        RecyclingBusinessJpa entity = mock(RecyclingBusinessJpa.class);
        RecyclingBusiness domain = mock(RecyclingBusiness.class);

        when(jpa.findByCnpj(cnpj.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<RecyclingBusiness> result = repository.findByCnpj(cnpj);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    @DisplayName("Should find all")
    void shouldFindAll() {
        RecyclingBusinessJpa entity = mock(RecyclingBusinessJpa.class);
        RecyclingBusiness domain = mock(RecyclingBusiness.class);

        when(jpa.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<RecyclingBusiness> result = repository.findAll();

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    @DisplayName("Should delete by ID")
    void shouldDelete() {
        UUID id = UUID.randomUUID();

        repository.delete(id);

        verify(jpa).deleteById(id);
    }
}
