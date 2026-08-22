package com.zera.ms_administrative_core.core.usecase.organization.registerOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.CnpjAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import java.util.UUID;

public class RegisterOrganizationImpl implements RegisterOrganization {
    private final OrganizationRepository organizationRepository;

    public RegisterOrganizationImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public RegisterOrganizationOutput execute(RegisterOrganizationCommand command) {
        Email email = command.email();

        if(organizationRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        Cnpj cnpj = command.cnpj();

        if(organizationRepository.existsByCnpj(cnpj)) {
            throw new CnpjAlreadyInUseException(cnpj);
        }

        Organization org = new Organization(
                UUID.randomUUID(),
                command.name(),
                command.cnpj(),
                Status.ACTIVE,
                command.email(),
                Plan.FREE
        );

        organizationRepository.save(org);
        return new RegisterOrganizationOutput(
                org.getOrganizationId(),
                org.getName(),
                org.getCnpj(),
                org.getEmail()
        );

    }
}
