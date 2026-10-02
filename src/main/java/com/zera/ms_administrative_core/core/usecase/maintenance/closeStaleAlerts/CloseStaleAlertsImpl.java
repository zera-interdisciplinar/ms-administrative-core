package com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;

@Service
public class CloseStaleAlertsImpl implements CloseStaleAlerts {

    /**
     * Limite superior de seguranca. Sem ele, um `dias` grande nao causa dano, mas um `dias`
     * pequeno vindo por engano (1) fecharia alerta do dia anterior em massa -- e alerta fechado
     * some da tela do gestor. O piso e a protecao que importa.
     */
    private static final int DIAS_MINIMO = 7;

    private final MaintenanceRepository repository;

    public CloseStaleAlertsImpl(MaintenanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public int execute(int dias) {
        if (dias < DIAS_MINIMO) {
            throw new IllegalArgumentException(
                    "Fechamento automatico exige pelo menos " + DIAS_MINIMO + " dias de inatividade");
        }
        return repository.closeStaleAlerts(dias);
    }
}
