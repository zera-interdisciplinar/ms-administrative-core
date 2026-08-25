package com.zera.ms_administrative_core.core.usecase.unit.renameUnit;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.repository.UnitRepository;

import java.util.UUID;

public class RenameUnitImpl implements RenameUnit {
    UnitRepository repository;

    public RenameUnitImpl(UnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID id, String newName) {
        Unit u = repository.findById(id).orElseThrow( () -> new UnitNotFoundException(id) );
        u.rename(newName);
        repository.save(u);
    }
}
