package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterOrganizationTelephoneImpl implements RegisterOrganizationTelephone {

    private final TelephoneRepository telephoneRepository;
    private final OrganizationRepository organizationRepository;

    public RegisterOrganizationTelephoneImpl(
            TelephoneRepository telephoneRepository,
            OrganizationRepository organizationRepository) {
        this.telephoneRepository = telephoneRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public RegisterTelephoneOutput execute(RegisterOrganizationTelephoneCommand command) {
        if (organizationRepository.findById(command.organizationId()).isEmpty()) {
            throw new OrganizationNotFoundException(command.organizationId());
        }

        if (telephoneRepository.findByOrganizationId(command.organizationId()).isPresent()) {
            throw TelephoneAlreadyRegisteredException.forOrganization(command.organizationId());
        }

        TelephoneNumber number = new TelephoneNumber(command.number());

        Telephone telephone = Telephone.forOrganization(
                UUID.randomUUID(),
                number,
                command.organizationId()
        );
        Telephone saved = telephoneRepository.save(telephone);

        return new RegisterTelephoneOutput(saved.getTelephoneId(), saved.getNumber().value());
    }
}
