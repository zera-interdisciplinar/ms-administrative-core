package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterOrganizationTelephoneImplTest {

    @Mock
    private TelephoneRepository telephoneRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private RegisterOrganizationTelephoneImpl useCase;

    private final UUID organizationId = UUID.randomUUID();
    private RegisterOrganizationTelephoneCommand command;
    private Organization organization;

    @BeforeEach
    void setUp() {
        command = new RegisterOrganizationTelephoneCommand(organizationId, "11987654321");
        organization = new Organization(organizationId, "Org", new Cnpj("11.222.333/0001-81"),
                Status.ACTIVE, new Email("org@email.com"), Plan.FREE);
    }

    @Test
    @DisplayName("Should register a telephone for an organization")
    void shouldRegister() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(telephoneRepository.findByOrganizationId(organizationId)).thenReturn(Optional.empty());
        when(telephoneRepository.save(any(Telephone.class))).thenAnswer(i -> i.getArgument(0));

        RegisterTelephoneOutput output = useCase.execute(command);

        assertEquals("11987654321", output.number());
        verify(telephoneRepository).save(any(Telephone.class));
    }

    @Test
    @DisplayName("Should fail when the organization does not exist")
    void shouldFailWhenOrganizationMissing() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the organization already has a telephone")
    void shouldFailWhenAlreadyRegistered() {
        Telephone existing = Telephone.forOrganization(UUID.randomUUID(), new TelephoneNumber("1133334444"), organizationId);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(telephoneRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(existing));

        assertThrows(TelephoneAlreadyRegisteredException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }
}
