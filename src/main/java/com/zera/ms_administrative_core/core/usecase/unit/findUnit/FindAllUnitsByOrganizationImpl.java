package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FindAllUnitsByOrganizationImpl implements FindAllUnitsByOrganization {

    private final UnitRepository repository;

    public FindAllUnitsByOrganizationImpl(UnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UnitOutput> execute(UUID organizationId, int page, int size) {
        return repository.findAll(organizationId, page, size).stream()
                .map(UnitOutput::from)
                .toList();
    }
}
