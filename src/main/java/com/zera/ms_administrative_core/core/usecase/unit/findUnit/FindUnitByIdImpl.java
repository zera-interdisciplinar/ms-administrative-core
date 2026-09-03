package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindUnitByIdImpl implements FindUnitById {

    private final UnitRepository repository;

    public FindUnitByIdImpl(UnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public UnitOutput execute(UUID unitId) {
        Unit unit = repository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException(unitId));

        return UnitOutput.from(unit);
    }
}
