package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.OrganizationJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.OrganizationMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationRepositoryImplTest {

    @Mock
    private OrganizationJpaRepository jpa;
    @Mock
    private OrganizationMapper mapper;

    @InjectMocks
    private OrganizationRepositoryImpl repository;

    private final Cnpj cnpj = new Cnpj("11.222.333/0001-81");
    private final Email email = new Email("org@email.com");

    @Test
    @DisplayName("Should save mapping to JPA and back to domain")
    void shouldSave() {
        Organization domain = mock(Organization.class);
        OrganizationJpa entity = mock(OrganizationJpa.class);
        OrganizationJpa saved = mock(OrganizationJpa.class);

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
        OrganizationJpa entity = mock(OrganizationJpa.class);
        Organization domain = mock(Organization.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findById(id));
    }

    @Test
    @DisplayName("Should find by CNPJ")
    void shouldFindByCnpj() {
        OrganizationJpa entity = mock(OrganizationJpa.class);
        Organization domain = mock(Organization.class);

        when(jpa.findByCnpj(cnpj.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findByCnpj(cnpj));
    }

    @Test
    @DisplayName("Should find by email")
    void shouldFindByEmail() {
        OrganizationJpa entity = mock(OrganizationJpa.class);
        Organization domain = mock(Organization.class);

        when(jpa.findByEmail(email.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(Optional.of(domain), repository.findByEmail(email));
    }

    @Test
    @DisplayName("Should delegate existsByCnpj / existsByEmail")
    void shouldDelegateExists() {
        when(jpa.existsByCnpj(cnpj.value())).thenReturn(true);
        when(jpa.existsByEmail(email.value())).thenReturn(false);

        assertTrue(repository.existsByCnpj(cnpj));
        assertFalse(repository.existsByEmail(email));
    }

    @Test
    @DisplayName("Should find all filtering by plan and status")
    void shouldFindAll() {
        OrganizationJpa entity = mock(OrganizationJpa.class);
        Organization domain = mock(Organization.class);

        when(jpa.findAllByPlanAndStatus(any(Plan.class), any(Status.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Organization> result = repository.findAll(Plan.FREE, Status.ACTIVE, 0, 20);

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
