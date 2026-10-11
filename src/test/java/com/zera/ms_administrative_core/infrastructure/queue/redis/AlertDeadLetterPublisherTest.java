package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertDeadLetterPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private final AlertQueueProperties properties = new AlertQueueProperties(
            "alert-notifications", "alert-notifications-dlq", "alert-notifications-cg",
            Duration.ofSeconds(30), 5);

    private AlertDeadLetterPublisher publisher;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        publisher = new AlertDeadLetterPublisher(redisTemplate, properties);
    }

    @Test
    @DisplayName("Deve publicar o payload original e o motivo na stream de DLQ configurada")
    void shouldPublishOriginalPayloadAndReasonOnTheDlqStream() {
        MapRecord<String, String, String> original = MapRecord.<String, String, String>create(
                        "alert-notifications", Map.of(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD, "{}"))
                .withId(RecordId.of("1-1"));

        publisher.publish(original, "usuario nao encontrado");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<MapRecord<String, String, String>> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        MapRecord<String, String, String> dlqRecord = captor.getValue();
        assertEquals("alert-notifications-dlq", dlqRecord.getStream());
        assertEquals("{}", dlqRecord.getValue().get(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD));
        assertEquals("usuario nao encontrado", dlqRecord.getValue().get("motivo"));
        assertEquals("1-1", dlqRecord.getValue().get("idOriginal"));
    }
}
