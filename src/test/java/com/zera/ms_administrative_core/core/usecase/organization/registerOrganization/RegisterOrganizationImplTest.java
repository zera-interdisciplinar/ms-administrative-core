package com.zera.ms_administrative_core.core.usecase.organization.registerOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.CnpjAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterOrganizationImplTest {

    @Mock
    private OrganizationRepository repository;

    @InjectMocks
    private RegisterOrganizationImpl useCase;

    private RegisterOrganizationCommand command;

    @BeforeEach
    void setUp() {
        command = new RegisterOrganizationCommand(
                "Org", new Cnpj("11.222.333/0001-81"), new Email("org@email.com"), Plan.FREE);
    }

    @Test
    @DisplayName("Should register organization and return its output")
    void shouldRegister() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByCnpj(command.cnpj())).thenReturn(false);

        RegisterOrganizationOutput output = useCase.execute(command);

        assertNotNull(output.id());
        assertEquals("Org", output.name());
        assertEquals(command.cnpj(), output.cnpj());
        assertEquals(command.email(), output.email());
        verify(repository).save(any(Organization.class));
    }

    @Test
    @DisplayName("Should reject when email is already in use")
    void shouldRejectDuplicateEmail() {
        when(repository.existsByEmail(command.email())).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(command));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when CNPJ is already in use")
    void shouldRejectDuplicateCnpj() {
        when(repository.existsByEmail(command.email())).thenReturn(false);
        when(repository.existsByCnpj(command.cnpj())).thenReturn(true);

        assertThrows(CnpjAlreadyInUseException.class, () -> useCase.execute(command));
        verify(repository, never()).save(any());
    }
}
