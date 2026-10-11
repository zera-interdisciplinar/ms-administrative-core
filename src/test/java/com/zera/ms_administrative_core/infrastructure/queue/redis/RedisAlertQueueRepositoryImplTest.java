package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAlertQueueRepositoryImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlertQueueProperties properties = new AlertQueueProperties(
            "alert-notifications", "alert-notifications-dlq", "alert-notifications-cg",
            Duration.ofSeconds(30), 5);

    private RedisAlertQueueRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        repository = new RedisAlertQueueRepositoryImpl(redisTemplate, objectMapper, properties);
    }

    @Test
    @DisplayName("Deve publicar o comando serializado como JSON na stream configurada")
    void shouldPublishTheCommandAsJsonOnTheConfiguredStream() {
        NotifyUserCommand command = new NotifyUserCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        repository.enqueue(command);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> record = captor.getValue();
        assertEquals("alert-notifications", record.getStream());
        String payload = record.getValue().get(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD);
        assertTrue(payload.contains(command.userId().toString()));
        assertTrue(payload.contains("STORAGE"));
    }
}
