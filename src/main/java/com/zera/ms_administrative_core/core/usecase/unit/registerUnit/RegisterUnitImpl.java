package com.zera.ms_administrative_core.core.usecase.unit.registerUnit;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.UnitOutput;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUnitImpl implements RegisterUnit {
    UnitRepository unitRepository;
    OrganizationRepository organizationRepository;

    public RegisterUnitImpl(UnitRepository unitRepository, OrganizationRepository organizationRepository) {
        this.unitRepository = unitRepository;
        this.organizationRepository = organizationRepository;
    }


    @Override
    public RegisterUnitOutput execute(RegisterUnitCommand command) {
        if (organizationRepository.findById(command.organizationId()).isEmpty()){
            throw new OrganizationNotFoundException(command.organizationId());
        }

        Unit u = new Unit(
                UUID.randomUUID(),
                command.name(),
                command.organizationId()
        );
        unitRepository.save(u);

        return new RegisterUnitOutput(u.getUnitId(), u.getName(), u.getOrganizationId());
    }
}
