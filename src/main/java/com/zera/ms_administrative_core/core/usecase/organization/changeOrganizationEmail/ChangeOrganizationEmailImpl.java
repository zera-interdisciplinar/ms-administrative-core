package com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChangeOrganizationEmailImpl implements ChangeOrganizationEmail {

    private final OrganizationRepository repository;

    public ChangeOrganizationEmailImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId, String newEmail) {
        Email email = new Email(newEmail);

        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (org.getEmail().equals(email) ) {
            return;
        }

        if (repository.existsByEmail(email)){
            throw new EmailAlreadyInUseException(email);
        }

        org.changeEmail(email);
        repository.save(org);
    }
}
