package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.repository.AlertQueueRepository;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

/**
 * Publica o comando na Stream Redis. Serializa o {@link NotifyUserCommand} inteiro como JSON num
 * unico campo ({@code payload}) em vez de um campo por atributo -- assim o shape do comando pode
 * mudar sem exigir migracao de formato de mensagem.
 */
@Component
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
public class RedisAlertQueueRepositoryImpl implements AlertQueueRepository {

    public static final String PAYLOAD_FIELD = "payload";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlertQueueProperties properties;

    public RedisAlertQueueRepositoryImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                          AlertQueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void enqueue(NotifyUserCommand command) {
        String json = objectMapper.writeValueAsString(command);
        redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .ofMap(Map.of(PAYLOAD_FIELD, json))
                        .withStreamKey(properties.stream()));
    }
}
