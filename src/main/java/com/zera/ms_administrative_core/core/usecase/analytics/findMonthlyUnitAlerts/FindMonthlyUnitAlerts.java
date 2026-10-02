package com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts;

import java.util.List;
import java.util.UUID;

import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;

public interface FindMonthlyUnitAlerts {
    List<MonthlyUnitAlerts> execute(UUID unidadeId);
}
