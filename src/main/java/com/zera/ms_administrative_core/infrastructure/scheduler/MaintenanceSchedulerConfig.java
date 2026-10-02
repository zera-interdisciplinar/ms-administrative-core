package com.zera.ms_administrative_core.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;

/**
 * Liga o agendador de manutencao. Desligado por padrao: em ambiente com mais de uma replica, N
 * instancias rodariam a mesma procedure ao mesmo tempo. As procedures sao idempotentes, entao o
 * resultado continua correto, mas o trabalho e desperdicado -- ligar em uma replica so, ou usar um
 * agendador externo chamando o endpoint, e o caminho recomendado.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MaintenanceProperties.class)
@ConditionalOnProperty(name = "zera.maintenance.enabled", havingValue = "true")
class MaintenanceSchedulerConfig {

    @Bean
    MaintenanceJob maintenanceJob(CloseStaleAlerts closeStaleAlerts,
                                  RevokeExpiredTokens revokeExpiredTokens,
                                  ConsolidateDau consolidateDau,
                                  MaintenanceProperties properties) {
        return new MaintenanceJob(closeStaleAlerts, revokeExpiredTokens, consolidateDau, properties);
    }
}
