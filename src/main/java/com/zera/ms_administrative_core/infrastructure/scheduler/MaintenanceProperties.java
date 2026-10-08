package com.zera.ms_administrative_core.infrastructure.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do agendador de manutencao.
 *
 * <p>{@code closeStaleAlertsEnabled} e FALSO por padrao, separado do resto de proposito:
 * {@code alert.status} e observado pelo ms-inventory, e fechar alerta automaticamente e decisao de
 * PRODUTO. Revogar token vencido e consolidar DAU sao higiene e podem rodar sozinhos; fechar alerta
 * do gestor sem ele pedir, nao.
 */
@ConfigurationProperties(prefix = "zera.maintenance")
public record MaintenanceProperties(
        boolean closeStaleAlertsEnabled,
        int closeStaleAlertsDays,
        int tokenRetentionDays,
        int dauConsolidationWindowDays
) {
    public MaintenanceProperties {
        if (closeStaleAlertsDays < 7) {
            closeStaleAlertsDays = 30;
        }
        if (tokenRetentionDays < 0) {
            tokenRetentionDays = 7;
        }
        if (dauConsolidationWindowDays < 1) {
            dauConsolidationWindowDays = 7;
        }
    }
}
