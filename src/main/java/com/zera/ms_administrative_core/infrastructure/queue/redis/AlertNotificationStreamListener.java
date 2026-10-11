package com.zera.ms_administrative_core.infrastructure.queue.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

/**
 * Consome a stream de alertas e chama o {@link NotifyUser} de sempre -- a regra de negocio
 * (validar usuario, deduplicar, persistir) nao muda, so passa a rodar fora do request HTTP.
 *
 * <p>{@code UserNotFoundException} e erro de negocio: o dado que chegou e invalido, reprocessar
 * nao resolve, entao vai direto pra DLQ com ACK. Qualquer outra excecao (timeout de conexao,
 * deadlock etc.) e tratada como falha transitoria: a mensagem fica sem ACK, pendente no consumer
 * group, para o {@link AlertQueueReclaimTask} decidir se reprocessa ou desiste.
 */
@Component
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
public class AlertNotificationStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationStreamListener.class);

    private final NotifyUser notifyUser;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AlertDeadLetterPublisher deadLetterPublisher;
    private final AlertQueueProperties properties;

    public AlertNotificationStreamListener(NotifyUser notifyUser, ObjectMapper objectMapper,
                                            StringRedisTemplate redisTemplate,
                                            AlertDeadLetterPublisher deadLetterPublisher,
                                            AlertQueueProperties properties) {
        this.notifyUser = notifyUser;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.deadLetterPublisher = deadLetterPublisher;
        this.properties = properties;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        try {
            String payload = record.getValue().get(RedisAlertQueueRepositoryImpl.PAYLOAD_FIELD);
            NotifyUserCommand command = objectMapper.readValue(payload, NotifyUserCommand.class);
            notifyUser.execute(command);
            ack(record);
        } catch (UserNotFoundException negocio) {
            deadLetterPublisher.publish(record, negocio.getMessage());
            ack(record);
        } catch (RuntimeException transitoria) {
            log.error("[ALERT-QUEUE] falha ao processar mensagem {}, sera reclamada depois", record.getId(),
                    transitoria);
        }
    }

    private void ack(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(properties.consumerGroup(), record);
    }
}
