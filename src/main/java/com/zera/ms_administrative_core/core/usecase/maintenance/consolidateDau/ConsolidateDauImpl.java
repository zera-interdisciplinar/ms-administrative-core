package com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;

@Service
public class ConsolidateDauImpl implements ConsolidateDau {

    private final MaintenanceRepository repository;

    public ConsolidateDauImpl(MaintenanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public int execute(LocalDate de, LocalDate ate) {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new IllegalArgumentException("Periodo invalido para consolidacao de DAU");
        }
        return repository.consolidateDau(de, ate);
    }
}
