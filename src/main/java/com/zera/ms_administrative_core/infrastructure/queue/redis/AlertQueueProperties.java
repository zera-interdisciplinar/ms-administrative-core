package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao da fila de alertas em Redis Streams.
 *
 * <p>{@code reclaimIdle} e {@code maxDelivery} sao palpites, nao valores medidos: ajustar depois de
 * observar o tempo real de processamento de {@code NotifyUser.execute}.
 */
@ConfigurationProperties(prefix = "zera.alerts.queue")
public record AlertQueueProperties(
        String stream,
        String dlqStream,
        String consumerGroup,
        Duration reclaimIdle,
        int maxDelivery
) {
    public AlertQueueProperties {
        if (stream == null || stream.isBlank()) {
            stream = "alert-notifications";
        }
        if (dlqStream == null || dlqStream.isBlank()) {
            dlqStream = "alert-notifications-dlq";
        }
        if (consumerGroup == null || consumerGroup.isBlank()) {
            consumerGroup = "alert-notifications-cg";
        }
        if (reclaimIdle == null || reclaimIdle.isNegative() || reclaimIdle.isZero()) {
            reclaimIdle = Duration.ofSeconds(30);
        }
        if (maxDelivery < 1) {
            maxDelivery = 5;
        }
    }
}
