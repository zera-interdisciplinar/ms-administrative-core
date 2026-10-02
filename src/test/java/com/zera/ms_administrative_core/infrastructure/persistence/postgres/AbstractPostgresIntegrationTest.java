package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;

/**
 * Base dos testes que precisam de Postgres DE VERDADE.
 *
 * <p>POR QUE NAO DA PARA USAR O H2 DOS TESTES UNITARIOS: nada do que estes testes verificam existe
 * no H2 -- PL/pgSQL, trigger com {@code TG_OP}, {@code JSONB}, heranca de tabelas, window function,
 * CTE recursiva, indice parcial. E pior: os testes unitarios rodam com
 * {@code ddl-auto=create-drop}, ou seja, nem sequer aplicam as migracoes. Um teste unitario passar
 * NAO prova que a migracao funciona; e esta hierarquia que prova.
 *
 * <p>O Postgres vem de {@link PostgresTestContainer}, um container unico para a suite inteira (ver
 * o javadoc de la para o motivo de nao usar {@code @Container}).
 *
 * <p>Sem Docker, as subclasses sao PULADAS em vez de falharem -- e por isso que
 * {@code ./mvnw verify} sem Docker nao prova nada sobre as migracoes.
 *
 * <h2>Regra para quem adicionar uma subclasse</h2>
 *
 * <p><b>Toda subclasse que ESCREVE precisa ser {@code @Transactional}</b>, para o rollback devolver
 * o banco ao estado anterior. Nao e preferencia de estilo: o container e o banco sao COMPARTILHADOS
 * por todas as classes, e algumas assercoes dependem disso. {@code BusinessProceduresIntegrationTest}
 * afirma que a segunda chamada de {@code sp_fechar_alertas_obsoletos} afeta ZERO linhas -- se outra
 * classe deixar um alerta OPEN antigo commitado, aquele teste quebra, e o erro vai apontar para o
 * lugar errado, longe da causa.
 *
 * <p>As unicas subclasses que podem dispensar {@code @Transactional} sao as que apenas LEEM
 * ({@code FlywayPostgresIntegrationTest}, {@code DataCatalogIntegrationTest}).
 */
@SpringBootTest
@EnabledIf(value = "dockerDisponivel", disabledReason = "Docker ausente: testes de Postgres pulados")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
public abstract class AbstractPostgresIntegrationTest {

    /** Avaliado ANTES de qualquer teste, e sem carregar {@link PostgresTestContainer}. */
    static boolean dockerDisponivel() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer.INSTANCE::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbc;

    /** Cria organizacao, unidade e gestor com ids unicos, devolvendo os ids para o teste usar. */
    protected Fixture novaHierarquia() {
        UUID orgId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID gestorId = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        String sufixo = orgId.toString().replace("-", "");

        jdbc.update("""
                INSERT INTO organization (id, name, cnpj, status, email, plan, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, 'BASIC', ?, ?)
                """, orgId, "Org " + sufixo, cnpjSintetico(), "org." + sufixo + "@test.local", agora, agora);

        jdbc.update("""
                INSERT INTO unit (id, name, organization_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, unitId, "Unidade " + sufixo.substring(0, 8), orgId, agora, agora);

        inserirUsuario(gestorId, unitId, null, "MANAGER", "ACTIVE");

        return new Fixture(orgId, unitId, gestorId);
    }

    protected void inserirUsuario(UUID id, UUID unitId, UUID managerId, String role, String status) {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        jdbc.update("""
                INSERT INTO user_account (id, name, role, password, email, status, unit_id,
                                          manager_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, role + " " + id.toString().substring(0, 8), role,
                "$2a$10$0123456789012345678901234567890123456789012345678",
                id + "@test.local", status, unitId, managerId, agora, agora);
    }

    protected UUID inserirAlerta(UUID unitId, UUID userId, String severidade, String status,
                                 LocalDateTime ocorridoEm) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO alert (id, status, unit_id, user_id, description, severity, kind,
                                   occurred_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'alerta de teste', ?, 'TEST_KIND', ?, ?, ?)
                """, id, status, unitId, userId, severidade, ocorridoEm, ocorridoEm,
                ocorridoEm.plusHours(2));
        return id;
    }

    /**
     * CNPJ sintetico valido. Nao serve numero aleatorio: o catalogo documenta que CNPJ deveria
     * passar por {@code fn_validar_cnpj}, e um fixture invalido esconderia uma regressao ali.
     */
    protected String cnpjSintetico() {
        String base = String.format("%012d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000L);
        int dv1 = digitoVerificador(base, 5);
        int dv2 = digitoVerificador(base + dv1, 6);
        return base + dv1 + dv2;
    }

    private int digitoVerificador(String digitos, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < digitos.length(); i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * peso;
            peso = (peso == 2) ? 9 : peso - 1;
        }
        int dv = 11 - (soma % 11);
        return dv >= 10 ? 0 : dv;
    }

    protected record Fixture(UUID organizacaoId, UUID unidadeId, UUID gestorId) {}
}
