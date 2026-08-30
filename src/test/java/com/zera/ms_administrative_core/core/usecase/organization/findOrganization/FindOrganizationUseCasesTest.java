package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindOrganizationUseCasesTest {

    @Mock
    private OrganizationRepository repository;

    private final UUID id = UUID.randomUUID();
    private final Cnpj cnpj = new Cnpj("11.222.333/0001-81");
    private final Email email = new Email("org@email.com");

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization(id, "Org", cnpj, Status.ACTIVE, email, Plan.FREE);
    }

    @Test
    @DisplayName("FindOrganizationById should return the mapped output")
    void shouldFindById() {
        when(repository.findById(id)).thenReturn(Optional.of(organization));

        OrganizationOutput output = new FindOrganizationByIdImpl(repository).execute(id);

        assertEquals(id, output.organizationId());
        assertEquals("Org", output.name());
        assertEquals(cnpj, output.cnpj());
        assertEquals(email, output.email());
        assertEquals(Plan.FREE, output.plan());
    }

    @Test
    @DisplayName("FindOrganizationById should fail when not found")
    void shouldFailById() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new FindOrganizationByIdImpl(repository).execute(id));
    }

    @Test
    @DisplayName("FindOrganizationByCnpj should return the mapped output")
    void shouldFindByCnpj() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(organization));

        OrganizationOutput output = new FindOrganizationByCnpjImpl(repository).execute("11.222.333/0001-81");

        assertEquals(id, output.organizationId());
    }

    @Test
    @DisplayName("FindOrganizationByCnpj should fail when not found")
    void shouldFailByCnpj() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new FindOrganizationByCnpjImpl(repository).execute("11.222.333/0001-81"));
    }

    @Test
    @DisplayName("FindOrganizationByEmail should return the mapped output")
    void shouldFindByEmail() {
        when(repository.findByEmail(email)).thenReturn(Optional.of(organization));

        OrganizationOutput output = new FindOrganizationByEmailImpl(repository).execute("org@email.com");

        assertEquals(id, output.organizationId());
    }

    @Test
    @DisplayName("FindOrganizationByEmail should fail when not found")
    void shouldFailByEmail() {
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new FindOrganizationByEmailImpl(repository).execute("org@email.com"));
    }

    @Test
    @DisplayName("FindAllOrganizations should map every organization returned by the repository")
    void shouldFindAll() {
        when(repository.findAll(Plan.FREE, Status.ACTIVE, 0, 20)).thenReturn(List.of(organization));

        List<OrganizationOutput> output = new FindAllOrganizationsImpl(repository)
                .execute(Plan.FREE, Status.ACTIVE, 0, 20);

        assertEquals(1, output.size());
        assertEquals(id, output.get(0).organizationId());
    }
}
