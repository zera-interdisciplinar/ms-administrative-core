package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertNotificationStreamListenerTest {

    @Mock
    private NotifyUser notifyUser;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private AlertDeadLetterPublisher deadLetterPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlertQueueProperties properties = new AlertQueueProperties(
            "alert-notifications", "alert-notifications-dlq", "alert-notifications-cg",
            Duration.ofSeconds(30), 5);

    private AlertNotificationStreamListener listener;

    @BeforeEach
    void setUp() {
        listener = new AlertNotificationStreamListener(notifyUser, objectMapper, redisTemplate,
                deadLetterPublisher, properties);
    }

    private MapRecord<String, String, String> record(NotifyUserCommand command) {
        String json = objectMapper.writeValueAsString(command);
        return MapRecord.<String, String, String>create("alert-notifications",
                        Map.of(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD, json))
                .withId(RecordId.of("1-1"));
    }

    private NotifyUserCommand umComando() {
        return new NotifyUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "storage almost full", Severity.HIGH, AlertKind.STORAGE,
                AlertStatus.OPEN, LocalDateTime.of(2024, 1, 1, 8, 0));
    }

    @Test
    @DisplayName("Deve chamar o NotifyUser e confirmar (ACK) quando o processamento for bem sucedido")
    void shouldCallNotifyUserAndAckOnSuccess() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        NotifyUserCommand command = umComando();
        MapRecord<String, String, String> record = record(command);

        listener.onMessage(record);

        verify(notifyUser).execute(command);
        verify(streamOperations).acknowledge(eq("alert-notifications-cg"), eq(record));
        verifyNoInteractions(deadLetterPublisher);
    }

    @Test
    @DisplayName("Deve mover para a DLQ e confirmar (ACK) quando o usuario nao existe")
    void shouldMoveToDlqAndAckOnBusinessError() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        NotifyUserCommand command = umComando();
        MapRecord<String, String, String> record = record(command);
        doThrow(new UserNotFoundException(command.userId())).when(notifyUser).execute(command);

        listener.onMessage(record);

        verify(deadLetterPublisher).publish(eq(record), any());
        verify(streamOperations).acknowledge(eq("alert-notifications-cg"), eq(record));
    }

    @Test
    @DisplayName("Nao deve confirmar (ACK) quando a falha for transitoria, para o reclaim tentar de novo")
    void shouldNotAckOnTransientFailure() {
        NotifyUserCommand command = umComando();
        MapRecord<String, String, String> record = record(command);
        doThrow(new DataAccessResourceFailureException("timeout")).when(notifyUser).execute(command);

        listener.onMessage(record);

        verify(notifyUser).execute(command);
        verifyNoInteractions(deadLetterPublisher);
        verify(redisTemplate, never()).opsForStream();
    }
}
