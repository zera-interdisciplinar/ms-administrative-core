package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FindAllTelephonesByOrganizationIdImpl implements FindAllTelephonesByOrganizationId {

    private final TelephoneRepository repository;

    public FindAllTelephonesByOrganizationIdImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TelephoneOutput> execute(UUID organizationId) {
        return repository.findAllByOrganizationId(organizationId).stream()
                .map(TelephoneOutput::from)
                .toList();
    }
}
