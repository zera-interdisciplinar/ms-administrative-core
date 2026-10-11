package com.zera.ms_administrative_core.infrastructure.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertDeadLetterPublisher;
import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertNotificationStreamListener;
import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertQueueProperties;

/**
 * Reclama mensagens que ficaram pendentes no consumer group por tempo demais -- ou porque o
 * worker que as recebeu morreu antes do ACK, ou porque {@code NotifyUser.execute} lancou uma falha
 * transitoria (ver {@link AlertNotificationStreamListener}).
 *
 * <p>{@code reclaimIdle} e {@code maxDelivery} vem de {@link AlertQueueProperties} e sao palpites
 * iniciais (30s / 5 tentativas), nao valores medidos.
 */
@Component
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
public class AlertQueueReclaimTask {

    private static final Logger log = LoggerFactory.getLogger(AlertQueueReclaimTask.class);
    private static final String RECLAIM_CONSUMER = "alert-notifications-reclaimer";

    private final StringRedisTemplate redisTemplate;
    private final AlertNotificationStreamListener listener;
    private final AlertDeadLetterPublisher deadLetterPublisher;
    private final AlertQueueProperties properties;

    public AlertQueueReclaimTask(StringRedisTemplate redisTemplate, AlertNotificationStreamListener listener,
                                  AlertDeadLetterPublisher deadLetterPublisher, AlertQueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.listener = listener;
        this.deadLetterPublisher = deadLetterPublisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${zera.alerts.queue.reclaim-interval:PT30S}")
    public void reclaim() {
        PendingMessages pendentes = streamOps()
                .pending(properties.stream(), properties.consumerGroup(), Range.unbounded(), 100);

        for (PendingMessage pendente : pendentes) {
            if (pendente.getElapsedTimeSinceLastDelivery().compareTo(properties.reclaimIdle()) < 0) {
                continue;
            }
            if (pendente.getTotalDeliveryCount() > properties.maxDelivery()) {
                desistir(pendente);
            } else {
                reclamarEReprocessar(pendente);
            }
        }
    }

    private void reclamarEReprocessar(PendingMessage pendente) {
        claim(pendente).forEach(listener::onMessage);
    }

    private void desistir(PendingMessage pendente) {
        claim(pendente).forEach(record -> {
            deadLetterPublisher.publish(record,
                    "excedeu " + properties.maxDelivery() + " tentativas de entrega");
            streamOps().acknowledge(properties.consumerGroup(), record);
        });

        log.warn("[ALERT-QUEUE] mensagem {} desistida apos {} tentativas", pendente.getIdAsString(),
                pendente.getTotalDeliveryCount());
    }

    private List<MapRecord<String, String, String>> claim(PendingMessage pendente) {
        return streamOps().claim(properties.stream(), properties.consumerGroup(), RECLAIM_CONSUMER,
                XClaimOptions.minIdle(properties.reclaimIdle()).ids(pendente.getIdAsString()));
    }

    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }
}
