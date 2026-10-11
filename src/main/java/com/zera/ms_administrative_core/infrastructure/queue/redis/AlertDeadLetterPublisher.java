package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Move uma mensagem para a stream de dead-letter. Usado quando o erro e de negocio (reprocessar
 * nao resolve, ex. {@code UserNotFoundException}) ou quando o numero de tentativas de reclaim
 * passa de {@code maxDelivery} -- nesses casos insistir so adiaria um alerta que ninguem vai
 * receber de qualquer forma.
 */
@Component
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
public class AlertDeadLetterPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertDeadLetterPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final AlertQueueProperties properties;

    public AlertDeadLetterPublisher(StringRedisTemplate redisTemplate, AlertQueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void publish(MapRecord<String, String, String> original, String motivo) {
        Map<String, String> dlqBody = Map.of(
                RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD,
                original.getValue().getOrDefault(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD, ""),
                "motivo", motivo,
                "idOriginal", original.getId().getValue());

        redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().ofMap(dlqBody).withStreamKey(properties.dlqStream()));

        log.warn("[ALERT-QUEUE] mensagem {} movida para a DLQ. motivo={}", original.getId(), motivo);
    }
}
