package com.zera.ms_administrative_core.core.usecase.organization.lifecycle;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.usecase.organization.activateOrganization.ActivateOrganizationImpl;
import com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization.DeactivateOrganizationImpl;
import com.zera.ms_administrative_core.core.usecase.organization.suspendOrganization.SuspendOrganizationImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationLifecycleUseCasesTest {

    @Mock
    private OrganizationRepository repository;

    private final UUID id = UUID.randomUUID();

    private Organization organization(Status status) {
        return new Organization(id, "Org", new Cnpj("11.222.333/0001-81"),
                status, new Email("org@email.com"), Plan.FREE);
    }

    @Test
    @DisplayName("ActivateOrganization should activate a suspended organization")
    void shouldActivate() {
        Organization org = organization(Status.SUSPENDED);
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new ActivateOrganizationImpl(repository).execute(id);

        assertEquals(Status.ACTIVE, org.getStatus());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("ActivateOrganization should fail when organization does not exist")
    void shouldFailActivateWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new ActivateOrganizationImpl(repository).execute(id));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("DeactivateOrganization should deactivate an active organization")
    void shouldDeactivate() {
        Organization org = organization(Status.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new DeactivateOrganizationImpl(repository).execute(id);

        assertEquals(Status.INACTIVE, org.getStatus());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("DeactivateOrganization should fail when organization does not exist")
    void shouldFailDeactivateWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new DeactivateOrganizationImpl(repository).execute(id));
    }

    @Test
    @DisplayName("SuspendOrganization should suspend an active organization")
    void shouldSuspend() {
        Organization org = organization(Status.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new SuspendOrganizationImpl(repository).execute(id);

        assertEquals(Status.SUSPENDED, org.getStatus());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("SuspendOrganization should fail when organization does not exist")
    void shouldFailSuspendWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new SuspendOrganizationImpl(repository).execute(id));
    }
}
