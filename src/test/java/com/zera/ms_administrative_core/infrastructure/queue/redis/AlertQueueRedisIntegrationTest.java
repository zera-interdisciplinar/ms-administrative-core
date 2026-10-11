package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.DockerClientFactory;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.repository.AlertQueueRepository;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prova a fiacao real com Redis Streams (XADD, consumer group, XACK, DLQ) -- nao a regra de
 * negocio do {@code NotifyUser}, que ja esta coberta em {@code NotifyUserImplTest} com mocks. Por
 * isso {@code NotifyUser} aqui tambem e mock: o que este teste precisa provar e que uma mensagem
 * publicada chega ao listener de verdade, via Redis de verdade, e que o ACK/DLQ acontecem.
 *
 * <p>Pulado sem Docker, como a suite de integracao do Postgres -- aqui nao ha modulo oficial de
 * Testcontainers para Redis (ver {@link RedisTestContainer}), so o container generico.
 */
@SpringBootTest
@EnabledIf(value = "dockerDisponivel", disabledReason = "Docker ausente: teste da fila Redis pulado")
@TestPropertySource(properties = "zera.alerts.queue.enabled=true")
class AlertQueueRedisIntegrationTest {

    static boolean dockerDisponivel() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisTestContainer.INSTANCE::getHost);
        registry.add("spring.data.redis.port", () -> RedisTestContainer.INSTANCE.getMappedPort(6379));
    }

    @Autowired
    private AlertQueueRepository alertQueueRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AlertQueueProperties properties;

    @MockitoBean
    private NotifyUser notifyUser;

    private NotifyUserCommand umComando() {
        return new NotifyUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "storage almost full", Severity.HIGH, AlertKind.STORAGE,
                AlertStatus.OPEN, LocalDateTime.of(2024, 1, 1, 8, 0));
    }

    @Test
    @DisplayName("Mensagem publicada na fila deve chegar ao NotifyUser via consumer group real")
    void shouldDeliverEnqueuedMessageToNotifyUser() {
        NotifyUserCommand command = umComando();

        alertQueueRepository.enqueue(command);

        verify(notifyUser, timeout(10_000)).execute(command);
    }

    @Test
    @DisplayName("Erro de negocio no processamento deve mover a mensagem para a DLQ")
    void shouldMoveToDlqOnBusinessError() {
        NotifyUserCommand command = umComando();
        doThrow(new UserNotFoundException(command.userId())).when(notifyUser).execute(command);

        alertQueueRepository.enqueue(command);
        verify(notifyUser, timeout(10_000)).execute(command);

        List<MapRecord<String, String, String>> dlq = aguardarDlq();
        assertThat(dlq).anySatisfy(record ->
                assertThat(record.getValue().get("payload")).contains(command.userId().toString()));
    }

    private List<MapRecord<String, String, String>> aguardarDlq() {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            List<MapRecord<String, String, String>> entradas = redisTemplate.<String, String>opsForStream()
                    .range(properties.dlqStream(), org.springframework.data.domain.Range.<String>unbounded());
            if (!entradas.isEmpty()) {
                return entradas;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return List.of();
    }
}
