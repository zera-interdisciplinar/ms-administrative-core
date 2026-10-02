package com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;

@Service
public class FindUnitHealthImpl implements FindUnitHealth {

    private final AnalyticsRepository analytics;
    private final UnitRepository units;

    public FindUnitHealthImpl(AnalyticsRepository analytics, UnitRepository units) {
        this.analytics = analytics;
        this.units = units;
    }

    @Override
    public BigDecimal execute(UUID unidadeId, LocalDate de, LocalDate ate) {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        if (units.findById(unidadeId).isEmpty()) {
            throw new UnitNotFoundException(unidadeId);
        }
        return analytics.unitHealthIndex(unidadeId, de, ate);
    }
}
