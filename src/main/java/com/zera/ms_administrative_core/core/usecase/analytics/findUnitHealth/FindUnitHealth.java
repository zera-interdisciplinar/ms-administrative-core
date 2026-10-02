package com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface FindUnitHealth {
    BigDecimal execute(UUID unidadeId, LocalDate de, LocalDate ate);
}
