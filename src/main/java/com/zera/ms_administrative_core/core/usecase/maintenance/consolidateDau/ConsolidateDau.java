package com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau;

import java.time.LocalDate;

public interface ConsolidateDau {
    /** @return quantidade de dias reescritos no rollup. */
    int execute(LocalDate de, LocalDate ate);
}
