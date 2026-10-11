package com.zera.ms_administrative_core.infrastructure.queue.redis;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/**
 * Cria o consumer group (idempotente -- roda a cada boot) e o container que entrega mensagens ao
 * {@link AlertNotificationStreamListener}.
 *
 * <p>O nome do consumer inclui um sufixo aleatorio de instancia: com mais de uma replica, dois
 * pods usando o mesmo nome de consumer dentro do group fariam o Redis achar que e o MESMO
 * consumidor reconectando, misturando o PEL (pending entries list) dos dois.
 */
@Configuration
@EnableConfigurationProperties(AlertQueueProperties.class)
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "true")
public class RedisStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConfig.class);

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> alertStreamContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redisTemplate,
            AlertNotificationStreamListener listener,
            AlertQueueProperties properties) {

        criarGroupSeNaoExiste(redisTemplate, properties);

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        String consumerName = "ms-administrative-core-" + UUID.randomUUID();
        container.receive(
                Consumer.from(properties.consumerGroup(), consumerName),
                StreamOffset.create(properties.stream(), ReadOffset.lastConsumed()),
                listener);

        return container;
    }

    private void criarGroupSeNaoExiste(StringRedisTemplate redisTemplate, AlertQueueProperties properties) {
        try {
            redisTemplate.opsForStream()
                    .createGroup(properties.stream(), ReadOffset.from("0"), properties.consumerGroup());
        } catch (RedisSystemException jaExiste) {
            // BUSYGROUP: grupo ja criado num boot anterior -- esperado, nao e erro
            log.debug("Consumer group {} ja existe na stream {}", properties.consumerGroup(), properties.stream());
        }
    }
}
