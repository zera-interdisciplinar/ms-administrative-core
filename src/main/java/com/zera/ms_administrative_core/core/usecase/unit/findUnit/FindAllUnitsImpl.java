package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindAllUnitsImpl implements FindAllUnits {

    private final UnitRepository repository;

    public FindAllUnitsImpl(UnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UnitOutput> execute() {
        return repository.findAll().stream()
                .map(UnitOutput::from)
                .toList();
    }
}
