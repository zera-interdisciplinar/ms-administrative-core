package com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts;

public interface CloseStaleAlerts {
    /** @return quantidade de alertas fechados. */
    int execute(int dias);
}
