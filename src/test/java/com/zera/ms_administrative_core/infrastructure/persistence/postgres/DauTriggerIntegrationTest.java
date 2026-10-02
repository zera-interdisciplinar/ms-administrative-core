package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova o registro automatico de DAU da V11.
 *
 * <p>Todo teste aqui insere em {@code refresh_token}, NUNCA em {@code user_access_log}. Isso e
 * deliberado: escrever direto no log provaria apenas que a tabela aceita INSERT. O que precisa ser
 * provado e que o acesso e contabilizado SEM a aplicacao pedir -- ou seja, que a automacao existe.
 */
@Transactional
class DauTriggerIntegrationTest extends AbstractPostgresIntegrationTest {

    private void emitirSessao(UUID usuarioId, LocalDateTime quando) {
        jdbc.update("""
                INSERT INTO refresh_token (id, user_id, token_hash, expires_at, revoked, created_at)
                VALUES (?, ?, ?, ?, FALSE, ?)
                """, UUID.randomUUID(), usuarioId,
                String.format("%064d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 1_000_000L),
                quando.plusDays(7), quando);
    }

    @Test
    @DisplayName("Emitir sessao deve registrar acesso sem a aplicacao escrever no log")
    void shouldRegisterAccessAutomatically() {
        Fixture f = novaHierarquia();
        LocalDateTime agora = LocalDateTime.now().withNano(0);

        emitirSessao(f.gestorId(), agora);

        Map<String, Object> acesso = jdbc.queryForMap("""
                SELECT origem, ocorrido_em FROM user_access_log WHERE user_id = ?
                """, f.gestorId());

        assertThat(acesso.get("origem")).isEqualTo("LOGIN");
    }

    @Test
    @DisplayName("Rollup diario deve contar o usuario e o acesso")
    void shouldUpdateDailyRollup() {
        Fixture f = novaHierarquia();
        LocalDate hoje = LocalDate.now();
        Map<String, Object> antes = rollup(hoje);

        emitirSessao(f.gestorId(), LocalDateTime.now().withNano(0));

        Map<String, Object> depois = rollup(hoje);
        assertThat(inteiro(depois, "usuarios_ativos")).isEqualTo(inteiro(antes, "usuarios_ativos") + 1);
        assertThat(inteiro(depois, "acessos")).isEqualTo(inteiro(antes, "acessos") + 1);
    }

    /**
     * DAU e COUNT(DISTINCT): o segundo acesso do mesmo usuario no mesmo dia incrementa `acessos` e
     * NAO incrementa `usuarios_ativos`. Um contador ingenuo erraria exatamente aqui.
     */
    @Test
    @DisplayName("Segundo acesso do mesmo usuario no mesmo dia nao deve dobrar o DAU")
    void shouldNotDoubleCountSameUserSameDay() {
        Fixture f = novaHierarquia();
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        Map<String, Object> antes = rollup(hoje);

        emitirSessao(f.gestorId(), agora);
        emitirSessao(f.gestorId(), agora.plusMinutes(1));
        emitirSessao(f.gestorId(), agora.plusMinutes(2));

        Map<String, Object> depois = rollup(hoje);
        assertThat(inteiro(depois, "usuarios_ativos")).isEqualTo(inteiro(antes, "usuarios_ativos") + 1);
        assertThat(inteiro(depois, "acessos")).isEqualTo(inteiro(antes, "acessos") + 3);
    }

    @Test
    @DisplayName("Usuarios distintos no mesmo dia devem somar no DAU")
    void shouldCountDistinctUsers() {
        Fixture f = novaHierarquia();
        UUID outro = UUID.randomUUID();
        inserirUsuario(outro, f.unidadeId(), f.gestorId(), "EMPLOYEE", "ACTIVE");
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        Map<String, Object> antes = rollup(hoje);

        emitirSessao(f.gestorId(), agora);
        emitirSessao(outro, agora);

        assertThat(inteiro(rollup(hoje), "usuarios_ativos"))
                .isEqualTo(inteiro(antes, "usuarios_ativos") + 2);
    }

    @Test
    @DisplayName("zera.access_origin deve distinguir REFRESH de LOGIN")
    void shouldHonourAccessOrigin() {
        Fixture f = novaHierarquia();
        jdbc.queryForObject("SELECT set_config('zera.access_origin', 'REFRESH', true)", String.class);

        emitirSessao(f.gestorId(), LocalDateTime.now().withNano(0));

        assertThat(jdbc.queryForObject(
                "SELECT origem FROM user_access_log WHERE user_id = ?", String.class, f.gestorId()))
                .isEqualTo("REFRESH");
    }

    /** Origem desconhecida nao pode violar o CHECK e derrubar o login. */
    @Test
    @DisplayName("Origem invalida deve cair para LOGIN em vez de quebrar o login")
    void shouldFallbackToLoginOnInvalidOrigin() {
        Fixture f = novaHierarquia();
        jdbc.queryForObject("SELECT set_config('zera.access_origin', 'COISA_ESTRANHA', true)",
                String.class);

        emitirSessao(f.gestorId(), LocalDateTime.now().withNano(0));

        assertThat(jdbc.queryForObject(
                "SELECT origem FROM user_access_log WHERE user_id = ?", String.class, f.gestorId()))
                .isEqualTo("LOGIN");
    }

    /**
     * O rollup incremental pode divergir sob concorrencia. A procedure de reconciliacao e a rede de
     * seguranca -- e este teste prova que ela CORRIGE, nao so que ela roda.
     */
    @Test
    @DisplayName("sp_consolidar_dau deve corrigir um rollup corrompido a partir do log")
    void shouldReconcileCorruptedRollup() {
        Fixture f = novaHierarquia();
        LocalDate hoje = LocalDate.now();
        emitirSessao(f.gestorId(), LocalDateTime.now().withNano(0));
        int correto = inteiro(rollup(hoje), "usuarios_ativos");

        // Corrompe o rollup de proposito.
        jdbc.update("UPDATE usuario_ativo_diario SET usuarios_ativos = 999, acessos = 999 WHERE dia = ?",
                hoje);
        assertThat(inteiro(rollup(hoje), "usuarios_ativos")).isEqualTo(999);

        // queryForObject e nao update: um CALL com parametro INOUT DEVOLVE uma linha, e o
        // jdbc.update() rejeita resultado com "A result was returned when none was expected".
        jdbc.queryForObject("CALL sp_consolidar_dau(?, ?, NULL)", Integer.class, hoje, hoje);

        assertThat(inteiro(rollup(hoje), "usuarios_ativos")).isEqualTo(correto);
    }

    private Map<String, Object> rollup(LocalDate dia) {
        var linhas = jdbc.queryForList(
                "SELECT usuarios_ativos, acessos FROM usuario_ativo_diario WHERE dia = ?", dia);
        return linhas.isEmpty() ? Map.of("usuarios_ativos", 0, "acessos", 0) : linhas.getFirst();
    }

    private int inteiro(Map<String, Object> linha, String coluna) {
        return ((Number) linha.get(coluna)).intValue();
    }
}
