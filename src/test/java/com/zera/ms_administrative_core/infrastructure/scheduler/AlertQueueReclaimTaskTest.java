package com.zera.ms_administrative_core.infrastructure.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertDeadLetterPublisher;
import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertNotificationStreamListener;
import com.zera.ms_administrative_core.infrastructure.queue.redis.AlertQueueProperties;
import com.zera.ms_administrative_core.infrastructure.queue.redis.RedisAlertQueueRepositoryImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertQueueReclaimTaskTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private AlertNotificationStreamListener listener;

    @Mock
    private AlertDeadLetterPublisher deadLetterPublisher;

    private final AlertQueueProperties properties = new AlertQueueProperties(
            "alert-notifications", "alert-notifications-dlq", "alert-notifications-cg",
            Duration.ofSeconds(30), 5);

    private AlertQueueReclaimTask task;

    @BeforeEach
    void setUp() {
        when(redisTemplate.<String, String>opsForStream()).thenReturn(streamOperations);
        task = new AlertQueueReclaimTask(redisTemplate, listener, deadLetterPublisher, properties);
    }

    private PendingMessage pendenteCom(String id, Duration idle, long tentativas) {
        return new PendingMessage(RecordId.of(id), Consumer.from("cg", "algum-worker"), idle, tentativas);
    }

    private MapRecord<String, String, String> reclamada(String id) {
        return MapRecord.<String, String, String>create("alert-notifications",
                        Map.of(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD, "{}"))
                .withId(RecordId.of(id));
    }

    @Test
    @DisplayName("Mensagem ainda dentro do tempo de idle nao deve ser reclamada")
    void shouldSkipMessagesStillWithinIdleWindow() {
        PendingMessage recente = pendenteCom("1-1", Duration.ofSeconds(1), 1);
        when(streamOperations.pending(eq("alert-notifications"), eq("alert-notifications-cg"),
                any(Range.class), anyLong()))
                .thenReturn(new PendingMessages("alert-notifications-cg", List.of(recente)));

        task.reclaim();

        verify(streamOperations, never()).claim(any(), any(), any(), any(XClaimOptions.class));
    }

    @Test
    @DisplayName("Mensagem presa dentro do limite de tentativas deve ser reclamada e reprocessada")
    void shouldReclaimAndReprocessMessageBelowDeliveryLimit() {
        PendingMessage presa = pendenteCom("2-1", Duration.ofMinutes(5), 2);
        MapRecord<String, String, String> reclamada = reclamada("2-1");
        when(streamOperations.pending(eq("alert-notifications"), eq("alert-notifications-cg"),
                any(Range.class), anyLong()))
                .thenReturn(new PendingMessages("alert-notifications-cg", List.of(presa)));
        when(streamOperations.claim(eq("alert-notifications"), eq("alert-notifications-cg"), any(),
                any(XClaimOptions.class)))
                .thenReturn(List.of(reclamada));

        task.reclaim();

        verify(listener).onMessage(reclamada);
        verify(deadLetterPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("Mensagem que excedeu o numero maximo de tentativas deve ir para a DLQ e ser confirmada")
    void shouldMoveMessageToDlqAfterExceedingMaxDeliveryAttempts() {
        PendingMessage excedida = pendenteCom("3-1", Duration.ofMinutes(5), 6);
        MapRecord<String, String, String> reclamada = reclamada("3-1");
        when(streamOperations.pending(eq("alert-notifications"), eq("alert-notifications-cg"),
                any(Range.class), anyLong()))
                .thenReturn(new PendingMessages("alert-notifications-cg", List.of(excedida)));
        when(streamOperations.claim(eq("alert-notifications"), eq("alert-notifications-cg"), any(),
                any(XClaimOptions.class)))
                .thenReturn(List.of(reclamada));

        task.reclaim();

        verify(deadLetterPublisher).publish(eq(reclamada), any());
        verify(streamOperations).acknowledge(eq("alert-notifications-cg"), eq(reclamada));
        verify(listener, never()).onMessage(any());
    }
}
