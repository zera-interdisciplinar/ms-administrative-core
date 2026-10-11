package com.zera.ms_administrative_core.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o agendamento do {@link AlertQueueReclaimTask}, so quando a fila esta ligada
 * ({@code zera.alerts.queue.enabled=true}) -- sem isso o reclaim rodaria contra um Redis que nem
 * existe no modo direto (fallback sincrono).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
class AlertQueueSchedulerConfig {
}
