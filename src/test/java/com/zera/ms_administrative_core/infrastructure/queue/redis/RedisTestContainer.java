package com.zera.ms_administrative_core.infrastructure.queue.redis;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Container Redis compartilhado pela suite de integracao da fila -- mesmo padrao singleton do
 * {@code PostgresTestContainer} (ver o javadoc de la para o motivo: {@code @Testcontainers} +
 * {@code @Container} numa classe-base para o container ao fim de cada classe, e a segunda classe
 * de integracao reinicia com porta nova enquanto o contexto Spring cacheado continua na antiga).
 *
 * <p>Nao ha modulo oficial de Testcontainers para Redis no classpath deste projeto (so o core,
 * trazido transitivamente pelo Postgres); por isso {@code GenericContainer} com a imagem padrao,
 * em vez de um {@code RedisContainer} dedicado.
 */
final class RedisTestContainer {

    static final GenericContainer<?> INSTANCE =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    static {
        INSTANCE.start();
    }

    private RedisTestContainer() {}
}
