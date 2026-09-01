package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UnitMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitRepositoryImplTest {

    @Mock
    private UnitJpaRepository jpa;
    @Mock
    private UnitMapper mapper;

    @InjectMocks
    private UnitRepositoryImpl repository;

    @Test
    @DisplayName("Should save mapping the domain to JPA")
    void shouldSave() {
        Unit domain = mock(Unit.class);
        UnitJpa entity = mock(UnitJpa.class);
        when(mapper.toJpa(domain)).thenReturn(entity);

        repository.save(domain);

        verify(jpa).save(entity);
    }

    @Test
    @DisplayName("Should find by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        UnitJpa entity = mock(UnitJpa.class);
        Unit domain = mock(Unit.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findById(id));
    }

    @Test
    @DisplayName("Should find all by organization ID")
    void shouldFindAll() {
        UUID organizationId = UUID.randomUUID();
        UnitJpa entity = mock(UnitJpa.class);
        Unit domain = mock(Unit.class);

        when(jpa.findAllByOrganizationId(eq(organizationId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Unit> result = repository.findAll(organizationId, 0, 20);

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
