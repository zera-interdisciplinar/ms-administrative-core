package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Roda todas as migrations Flyway contra um Postgres real (Testcontainers). O H2 dos testes
 * unitarios nao exercita indices parciais, {@code gen_random_uuid()} nem os {@code CHECK} — este
 * teste sim, fechando o gap de schema-drift entre o que os testes veem e o que produz.
 *
 * <p>Pulado onde nao ha Docker ({@code disabledWithoutDocker}); roda no CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
class FlywayPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplyCleanlyOnRealPostgres() {
        var applied = flyway.info().applied();

        assertThat(applied).hasSizeGreaterThanOrEqualTo(7);
        assertThat(applied).allMatch(m -> m.getState() == MigrationState.SUCCESS);
    }

    @Test
    void postgresSpecificObjectsExist() {
        // indice parcial da V5 (dedup de alerta OPEN) — H2 nao suporta indice com WHERE
        Integer alertIdx = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'ux_alert_open_rule_event'",
                Integer.class);
        assertThat(alertIdx).isEqualTo(1);

        // indice parcial da V6 (convite PENDING unico por codigo)
        Integer inviteIdx = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'ux_invitation_pending_code'",
                Integer.class);
        assertThat(inviteIdx).isEqualTo(1);

        // tabela da V7
        Integer refreshTable = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'refresh_token'",
                Integer.class);
        assertThat(refreshTable).isEqualTo(1);
    }
}
