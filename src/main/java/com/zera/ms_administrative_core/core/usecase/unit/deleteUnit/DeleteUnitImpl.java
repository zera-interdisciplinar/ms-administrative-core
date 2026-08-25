package com.zera.ms_administrative_core.core.usecase.unit.deleteUnit;

import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeleteUnitImpl implements DeleteUnit {
    UnitRepository unitRepository;

    public DeleteUnitImpl(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    public void execute(UUID id) {
        unitRepository.delete(id);
    }
}
