package com.zera.ms_administrative_core.infrastructure.scheduler;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;

/**
 * Executa as procedures de manutencao periodicamente.
 *
 * <p>Cada tarefa e envolvida no proprio try/catch: uma falha na revogacao de token nao pode impedir
 * a consolidacao de DAU de rodar. Sem isso, a primeira excecao cancela o restante do ciclo e o
 * problema aparece dias depois como "o DAU parou de atualizar", longe da causa.
 */
public class MaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceJob.class);

    private final CloseStaleAlerts closeStaleAlerts;
    private final RevokeExpiredTokens revokeExpiredTokens;
    private final ConsolidateDau consolidateDau;
    private final MaintenanceProperties properties;

    public MaintenanceJob(CloseStaleAlerts closeStaleAlerts,
                          RevokeExpiredTokens revokeExpiredTokens,
                          ConsolidateDau consolidateDau,
                          MaintenanceProperties properties) {
        this.closeStaleAlerts = closeStaleAlerts;
        this.revokeExpiredTokens = revokeExpiredTokens;
        this.consolidateDau = consolidateDau;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${zera.maintenance.interval:PT1H}",
               initialDelayString = "${zera.maintenance.initial-delay:PT1M}")
    public void run() {
        revogarTokens();
        consolidarDau();
        fecharAlertas();
    }

    private void revogarTokens() {
        try {
            TokenCleanup resultado = revokeExpiredTokens.execute(properties.tokenRetentionDays());
            log.info("Manutencao: {} tokens revogados, {} removidos",
                    resultado.revogados(), resultado.removidos());
        } catch (RuntimeException e) {
            log.error("Manutencao: falha ao revogar tokens expirados", e);
        }
    }

    private void consolidarDau() {
        try {
            LocalDate ate = LocalDate.now();
            LocalDate de = ate.minusDays(properties.dauConsolidationWindowDays());
            log.info("Manutencao: DAU reconsolidado em {} dias", consolidateDau.execute(de, ate));
        } catch (RuntimeException e) {
            log.error("Manutencao: falha ao consolidar DAU", e);
        }
    }

    private void fecharAlertas() {
        if (!properties.closeStaleAlertsEnabled()) {
            return;
        }
        try {
            log.info("Manutencao: {} alertas obsoletos fechados",
                    closeStaleAlerts.execute(properties.closeStaleAlertsDays()));
        } catch (RuntimeException e) {
            log.error("Manutencao: falha ao fechar alertas obsoletos", e);
        }
    }
}
