package com.zera.ms_administrative_core.infrastructure.legacysync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zera.ms_administrative_core.infrastructure.legacysync.LegacyWriter.GestorChanges;

/**
 * Roda no H2: valida a montagem do UPDATE condicional e o efeito dele linha a linha. Nao prova o
 * comportamento no Postgres do legado (tipos, constraints, triggers dele) -- isso so se confirma
 * contra o banco real.
 */
class JdbcLegacyGatewayTest {

    private static final OffsetDateTime READ = OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
    private static final OffsetDateTime NEW = READ.plusHours(2);

    private JdbcLegacyGateway gateway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:legacy-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(1);
        HikariDataSource dataSource = new HikariDataSource(config);
        gateway = new JdbcLegacyGateway(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE gestor (codigo INT PRIMARY KEY, nome VARCHAR(100), email VARCHAR(100),
                    senha VARCHAR(100), telefone VARCHAR(11), cod_unidade INT,
                    criado_em TIMESTAMP WITH TIME ZONE, atualizado_em TIMESTAMP WITH TIME ZONE)
                """);
        jdbc.update("INSERT INTO gestor VALUES (1, 'Maria', 'm@b.com', 'hash', '11999998888', 5, ?, ?)", READ, READ);
        jdbc.update("INSERT INTO gestor VALUES (2, 'Joao', 'j@b.com', 'hash', NULL, 5, ?, NULL)", READ);
    }

    @AfterEach
    void tearDown() {
        gateway.close();
    }

    private Map<String, Object> row(int codigo) {
        return jdbc.queryForMap("SELECT nome, email, senha, telefone, cod_unidade, atualizado_em FROM gestor WHERE codigo = ?",
                codigo);
    }

    @Test
    @DisplayName("Should build the UPDATE with only the requested columns")
    void shouldBuildSqlWithRequestedColumnsOnly() {
        assertThat(JdbcLegacyGateway.updateGestorSql(new GestorChanges("a", null, null, "1"), false))
                .isEqualTo("UPDATE gestor SET nome = ?, telefone = ?, atualizado_em = ? "
                        + "WHERE codigo = ? AND atualizado_em = ?");
        assertThat(JdbcLegacyGateway.updateGestorSql(new GestorChanges(null, "e", "s", null), true))
                .isEqualTo("UPDATE gestor SET email = ?, senha = ?, atualizado_em = ? "
                        + "WHERE codigo = ? AND atualizado_em IS NULL");
    }

    @Test
    @DisplayName("Should update only the requested columns and stamp atualizado_em")
    void shouldUpdateOnlyRequestedColumns() {
        boolean updated = gateway.updateGestor(1, new GestorChanges("Maria Souza", null, null, "11888887777"),
                NEW, READ);

        assertThat(updated).isTrue();
        Map<String, Object> row = row(1);
        assertThat(row.get("NOME")).isEqualTo("Maria Souza");
        assertThat(row.get("TELEFONE")).isEqualTo("11888887777");
        assertThat(row.get("EMAIL")).isEqualTo("m@b.com");
        assertThat(row.get("SENHA")).isEqualTo("hash");
        assertThat(row.get("COD_UNIDADE")).isEqualTo(5);
        assertThat(((OffsetDateTime) row.get("ATUALIZADO_EM")).toInstant()).isEqualTo(NEW.toInstant());
    }

    @Test
    @DisplayName("Should discard the write when the legacy row changed after it was read")
    void shouldDiscardWriteWhenRowChangedMeanwhile() {
        boolean updated = gateway.updateGestor(1, new GestorChanges("Maria Souza", null, null, null),
                NEW, READ.minusMinutes(5));

        assertThat(updated).isFalse();
        assertThat(row(1).get("NOME")).isEqualTo("Maria");
    }

    @Test
    @DisplayName("Should write a row whose atualizado_em was null when it was read")
    void shouldWriteWhenExpectedTimestampIsNull() {
        boolean updated = gateway.updateGestor(2, new GestorChanges(null, "novo@b.com", null, null), NEW, null);

        assertThat(updated).isTrue();
        assertThat(row(2).get("EMAIL")).isEqualTo("novo@b.com");
    }

    @Test
    @DisplayName("Should not write a null-expected update over a row that now has atualizado_em")
    void shouldDiscardNullExpectedWhenRowNowHasTimestamp() {
        boolean updated = gateway.updateGestor(1, new GestorChanges("X", null, null, null), NEW, null);

        assertThat(updated).isFalse();
    }

    @Test
    @DisplayName("Should do nothing for an empty change set")
    void shouldIgnoreEmptyChanges() {
        assertThat(gateway.updateGestor(1, new GestorChanges(null, null, null, null), NEW, READ)).isFalse();
    }

    @Test
    @DisplayName("Should return false for a codigo that does not exist")
    void shouldReturnFalseForUnknownCodigo() {
        assertThat(gateway.updateGestor(99, new GestorChanges("X", null, null, null), NEW, READ)).isFalse();
    }
}
