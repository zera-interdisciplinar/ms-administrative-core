package com.zera.ms_administrative_core.core.usecase.unit.registerUnit;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;

import java.util.UUID;

public class RegisterUnitImpl implements RegisterUnit {
    UnitRepository unitRepository;
    OrganizationRepository organizationRepository;

    public RegisterUnitImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }


    @Override
    public void execute(RegisterUnitCommand command) {
        if (organizationRepository.findById(command.organizationId()).isEmpty()){
            throw new OrganizationNotFoundException(command.organizationId());
        }

        Unit u = new Unit(
                UUID.randomUUID(),
                command.name(),
                command.organizationId()
        );
        unitRepository.save(u);
    }
}
