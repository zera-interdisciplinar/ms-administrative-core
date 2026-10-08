package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Container Postgres compartilhado por TODA a suite de integracao (padrao singleton container).
 *
 * <p>POR QUE NAO {@code @Testcontainers} + {@code @Container} NUMA CLASSE-BASE: a extensao JUnit
 * PARA o container estatico ao fim de CADA classe de teste. Com varias subclasses, a segunda tenta
 * usar um container ja parado; ao reinicia-lo o Testcontainers cria um container NOVO, com PORTA
 * NOVA, enquanto o contexto Spring (que e cacheado entre classes) continua apontando para a porta
 * antiga. O sintoma e enganoso -- {@code Connection refused} e timeout de pool de 30s por teste --
 * e nao aparece se houver apenas uma classe de integracao, o que faz o problema surgir so quando a
 * segunda e escrita.
 *
 * <p>Aqui o container e iniciado uma unica vez, na carga desta classe, e NUNCA parado
 * explicitamente: quem o remove e o Ryuk (container sentinela do Testcontainers) ao fim da JVM.
 *
 * <p>Esta classe fica separada da classe-base de proposito: a base precisa poder responder "ha
 * Docker aqui?" SEM carregar esta classe, senao o bloco estatico abaixo estouraria antes de o teste
 * ser pulado.
 */
final class PostgresTestContainer {

    static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {}
}
