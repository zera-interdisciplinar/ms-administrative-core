package com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;

@Service
public class FindMonthlyUnitAlertsImpl implements FindMonthlyUnitAlerts {

    private final AnalyticsRepository analytics;
    private final UnitRepository units;

    public FindMonthlyUnitAlertsImpl(AnalyticsRepository analytics, UnitRepository units) {
        this.analytics = analytics;
        this.units = units;
    }

    @Override
    public List<MonthlyUnitAlerts> execute(UUID unidadeId) {
        // Unidade inexistente e 404, nao lista vazia: lista vazia diria ao gestor "essa unidade
        // nao teve alerta", o que e uma afirmacao diferente de "essa unidade nao existe".
        if (units.findById(unidadeId).isEmpty()) {
            throw new UnitNotFoundException(unidadeId);
        }
        return analytics.monthlyAlertsByUnit(unidadeId);
    }
}
