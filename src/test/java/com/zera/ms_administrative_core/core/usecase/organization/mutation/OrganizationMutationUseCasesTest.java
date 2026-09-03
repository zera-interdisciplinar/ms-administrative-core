package com.zera.ms_administrative_core.core.usecase.organization.mutation;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail.ChangeOrganizationEmailImpl;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationPlan.ChangeOrganizationPlanImpl;
import com.zera.ms_administrative_core.core.usecase.organization.renameOrganization.RenameOrganizationImpl;
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
class OrganizationMutationUseCasesTest {

    @Mock
    private OrganizationRepository repository;

    private final UUID id = UUID.randomUUID();

    private Organization organization() {
        return new Organization(id, "Org", new Cnpj("11.222.333/0001-81"),
                Status.ACTIVE, new Email("org@email.com"), Plan.FREE);
    }

    // --- RenameOrganization ---

    @Test
    @DisplayName("RenameOrganization should rename and persist")
    void shouldRename() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new RenameOrganizationImpl(repository).execute(id, "New Org");

        assertEquals("New Org", org.getName());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("RenameOrganization should be a no-op when the name is unchanged")
    void shouldSkipRenameWhenSameName() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new RenameOrganizationImpl(repository).execute(id, "Org");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("RenameOrganization should fail when organization does not exist")
    void shouldFailRenameWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new RenameOrganizationImpl(repository).execute(id, "New Org"));
    }

    // --- ChangeOrganizationEmail ---

    @Test
    @DisplayName("ChangeOrganizationEmail should change the email when the new one is free")
    void shouldChangeEmail() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));
        when(repository.existsByEmail(new Email("new@email.com"))).thenReturn(false);

        new ChangeOrganizationEmailImpl(repository).execute(id, "new@email.com");

        assertEquals(new Email("new@email.com"), org.getEmail());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("ChangeOrganizationEmail should be a no-op when the email is unchanged")
    void shouldSkipEmailWhenSame() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new ChangeOrganizationEmailImpl(repository).execute(id, "org@email.com");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ChangeOrganizationEmail should reject an email already in use")
    void shouldRejectEmailInUse() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));
        when(repository.existsByEmail(new Email("new@email.com"))).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class,
                () -> new ChangeOrganizationEmailImpl(repository).execute(id, "new@email.com"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ChangeOrganizationEmail should fail when organization does not exist")
    void shouldFailEmailWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new ChangeOrganizationEmailImpl(repository).execute(id, "new@email.com"));
    }

    // --- ChangeOrganizationPlan ---

    @Test
    @DisplayName("ChangeOrganizationPlan should change the plan and persist")
    void shouldChangePlan() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new ChangeOrganizationPlanImpl(repository).execute(id, Plan.PRO);

        assertEquals(Plan.PRO, org.getPlan());
        verify(repository).save(org);
    }

    @Test
    @DisplayName("ChangeOrganizationPlan should be a no-op when the plan is unchanged")
    void shouldSkipPlanWhenSame() {
        Organization org = organization();
        when(repository.findById(id)).thenReturn(Optional.of(org));

        new ChangeOrganizationPlanImpl(repository).execute(id, Plan.FREE);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ChangeOrganizationPlan should fail when organization does not exist")
    void shouldFailPlanWhenMissing() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new ChangeOrganizationPlanImpl(repository).execute(id, Plan.PRO));
    }
}
