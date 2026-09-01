package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.TelephoneJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.TelephoneMapper;
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
class TelephoneRepositoryImplTest {

    @Mock
    private TelephoneJpaRepository jpa;
    @Mock
    private TelephoneMapper mapper;

    @InjectMocks
    private TelephoneRepositoryImpl repository;

    @Test
    @DisplayName("Should save mapping to JPA and back to domain")
    void shouldSave() {
        Telephone domain = mock(Telephone.class);
        TelephoneJpa entity = mock(TelephoneJpa.class);
        TelephoneJpa saved = mock(TelephoneJpa.class);

        when(mapper.toJpa(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(domain);

        assertEquals(domain, repository.save(domain));
        verify(jpa).save(entity);
    }

    @Test
    @DisplayName("Should find by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        TelephoneJpa entity = mock(TelephoneJpa.class);
        Telephone domain = mock(Telephone.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findById(id));
    }

    @Test
    @DisplayName("Should find by user ID")
    void shouldFindByUserId() {
        UUID id = UUID.randomUUID();
        TelephoneJpa entity = mock(TelephoneJpa.class);
        Telephone domain = mock(Telephone.class);

        when(jpa.findByUserId(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findByUserId(id));
    }

    @Test
    @DisplayName("Should find by recycling business ID")
    void shouldFindByRecyclingBusinessId() {
        UUID id = UUID.randomUUID();
        TelephoneJpa entity = mock(TelephoneJpa.class);
        Telephone domain = mock(Telephone.class);

        when(jpa.findByRecyclingBusinessId(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findByRecyclingBusinessId(id));
    }

    @Test
    @DisplayName("Should delete mapping the domain to JPA")
    void shouldDelete() {
        Telephone domain = mock(Telephone.class);
        TelephoneJpa entity = mock(TelephoneJpa.class);
        when(mapper.toJpa(domain)).thenReturn(entity);

        repository.delete(domain);

        verify(jpa).delete(entity);
    }

    @Test
    @DisplayName("Should find all paginated")
    void shouldFindAll() {
        TelephoneJpa entity = mock(TelephoneJpa.class);
        Telephone domain = mock(Telephone.class);

        when(jpa.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Telephone> result = repository.findAll(0, 20);

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    @DisplayName("Should find all by organization ID")
    void shouldFindAllByOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        TelephoneJpa entity = mock(TelephoneJpa.class);
        Telephone domain = mock(Telephone.class);

        when(jpa.findAllByOrganizationId(eq(organizationId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Telephone> result = repository.findAllByOrganizationId(organizationId, 0, 20);

        assertEquals(1, result.size());
    }
}
