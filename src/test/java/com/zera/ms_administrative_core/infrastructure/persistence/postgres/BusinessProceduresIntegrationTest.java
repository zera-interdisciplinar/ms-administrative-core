package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;
import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;

/**
 * Prova as procedures da V13 E o adaptador que as aciona.
 *
 * <p>Vai pelo {@link MaintenanceRepository} de proposito, nao por SQL solto: o que precisa ser
 * provado inclui o {@code CallableStatement} com parametro {@code INOUT}, que e justamente a parte
 * que um teste com {@code CALL sp(?, NULL)} em SQL cru NAO exercitaria.
 */
@Transactional
class BusinessProceduresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MaintenanceRepository maintenance;

    // ------------------------------------------- sp_fechar_alertas_obsoletos

    @Test
    @DisplayName("Deve fechar alerta OPEN antigo e devolver a contagem pelo parametro INOUT")
    void shouldCloseStaleAlerts() {
        Fixture f = novaHierarquia();
        UUID antigo = inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.now().minusDays(60).withNano(0));

        int fechados = maintenance.closeStaleAlerts(30);

        assertThat(fechados).isGreaterThanOrEqualTo(1);
        assertThat(statusDoAlerta(antigo)).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Nao deve fechar alerta recente")
    void shouldNotCloseRecentAlert() {
        Fixture f = novaHierarquia();
        UUID recente = inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.now().minusDays(2).withNano(0));

        maintenance.closeStaleAlerts(30);

        assertThat(statusDoAlerta(recente)).isEqualTo("OPEN");
    }

    /** Idempotencia e o que permite reagendar sem medo depois de uma falha no meio. */
    @Test
    @DisplayName("Segunda execucao seguida deve afetar zero linhas")
    void shouldBeIdempotent() {
        Fixture f = novaHierarquia();
        inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.now().minusDays(60).withNano(0));

        maintenance.closeStaleAlerts(30);

        assertThat(maintenance.closeStaleAlerts(30)).isZero();
    }

    /**
     * A procedure usa `occurred_at`, nao `created_at`: o que importa e a idade do EVENTO. Um alerta
     * reprocessado hoje para um evento de 60 dias atras deve ser fechado mesmo tendo linha nova.
     */
    @Test
    @DisplayName("Deve usar a idade do evento, nao a idade da linha")
    void shouldUseEventAgeNotRowAge() {
        Fixture f = novaHierarquia();
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO alert (id, status, unit_id, user_id, description, severity, kind,
                                   occurred_at, created_at, updated_at)
                VALUES (?, 'OPEN', ?, ?, 'reprocessado', 'HIGH', 'TEST_KIND', ?, ?, ?)
                """, id, f.unidadeId(), f.gestorId(),
                LocalDateTime.now().minusDays(90).withNano(0),
                LocalDateTime.now().withNano(0), LocalDateTime.now().withNano(0));

        maintenance.closeStaleAlerts(30);

        assertThat(statusDoAlerta(id)).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Deve registrar a execucao em job_execucao")
    void shouldRecordJobExecution() {
        Fixture f = novaHierarquia();
        inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "OPEN",
                LocalDateTime.now().minusDays(60).withNano(0));

        maintenance.closeStaleAlerts(30);

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM job_execucao
                 WHERE job = 'sp_fechar_alertas_obsoletos' AND sucesso = TRUE
                """, Integer.class)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Procedure deve recusar janela menor que 1 dia")
    void shouldRejectInvalidWindowAtDatabaseLevel() {
        assertThatThrownBy(() -> jdbc.update("CALL sp_fechar_alertas_obsoletos(0, NULL)"))
                .hasMessageContaining("p_dias deve ser >= 1");
    }

    /** Fechamento automatico deixa rastro no proprio alerta, senao o gestor nao entende o sumico. */
    @Test
    @DisplayName("Deve carimbar o motivo do fechamento em notes")
    void shouldStampReasonInNotes() {
        Fixture f = novaHierarquia();
        UUID antigo = inserirAlerta(f.unidadeId(), f.gestorId(), "MEDIUM", "OPEN",
                LocalDateTime.now().minusDays(60).withNano(0));

        maintenance.closeStaleAlerts(30);

        assertThat(jdbc.queryForObject("SELECT notes FROM alert WHERE id = ?", String.class, antigo))
                .contains("Fechado automaticamente");
    }

    // ---------------------------------------- sp_revogar_tokens_expirados

    @Test
    @DisplayName("Deve revogar token vencido e preservar token valido")
    void shouldRevokeOnlyExpiredTokens() {
        Fixture f = novaHierarquia();
        UUID vencido = inserirToken(f.gestorId(), LocalDateTime.now().minusDays(1), false);
        UUID valido = inserirToken(f.gestorId(), LocalDateTime.now().plusDays(3), false);

        TokenCleanup resultado = maintenance.revokeExpiredTokens(30);

        assertThat(resultado.revogados()).isGreaterThanOrEqualTo(1);
        assertThat(revogado(vencido)).isTrue();
        assertThat(revogado(valido)).isFalse();
    }

    /**
     * Revogar e remover sao passos separados de proposito: entre os dois fica a janela em que ainda
     * da para investigar a sessao. Com retencao de 30 dias, um token vencido ontem NAO pode sumir.
     */
    @Test
    @DisplayName("Retencao deve impedir a remocao imediata do token recem-vencido")
    void shouldKeepRecentlyExpiredTokenWithinRetention() {
        Fixture f = novaHierarquia();
        UUID vencido = inserirToken(f.gestorId(), LocalDateTime.now().minusDays(1), false);

        maintenance.revokeExpiredTokens(30);

        assertThat(existe(vencido)).isTrue();
    }

    @Test
    @DisplayName("Deve remover token vencido alem da janela de retencao")
    void shouldDeleteTokenBeyondRetention() {
        Fixture f = novaHierarquia();
        UUID muitoAntigo = inserirToken(f.gestorId(), LocalDateTime.now().minusDays(60), true);

        TokenCleanup resultado = maintenance.revokeExpiredTokens(7);

        assertThat(resultado.removidos()).isGreaterThanOrEqualTo(1);
        assertThat(existe(muitoAntigo)).isFalse();
    }

    @Test
    @DisplayName("Procedure de tokens deve recusar retencao negativa")
    void shouldRejectNegativeRetentionAtDatabaseLevel() {
        assertThatThrownBy(() -> jdbc.update("CALL sp_revogar_tokens_expirados(-1, NULL, NULL)"))
                .hasMessageContaining("p_dias_retencao deve ser >= 0");
    }

    // ------------------------------------------------- sp_consolidar_dau

    @Test
    @DisplayName("Deve consolidar o DAU e devolver quantos dias reescreveu")
    void shouldConsolidateDau() {
        Fixture f = novaHierarquia();
        inserirToken(f.gestorId(), LocalDateTime.now().plusDays(7), false);

        int dias = maintenance.consolidateDau(LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(dias).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Procedure de DAU deve recusar periodo invertido")
    void shouldRejectInvertedPeriodAtDatabaseLevel() {
        assertThatThrownBy(() -> jdbc.update("CALL sp_consolidar_dau(?, ?, NULL)",
                LocalDate.now(), LocalDate.now().minusDays(3)))
                .hasMessageContaining("Periodo invalido");
    }

    private UUID inserirToken(UUID usuarioId, LocalDateTime expiraEm, boolean revogado) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO refresh_token (id, user_id, token_hash, expires_at, revoked, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, usuarioId,
                String.format("%064d", Math.abs(id.getLeastSignificantBits()) % 1_000_000_000L),
                expiraEm, revogado, LocalDateTime.now().withNano(0));
        return id;
    }

    private String statusDoAlerta(UUID id) {
        return jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, id);
    }

    private boolean revogado(UUID id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT revoked FROM refresh_token WHERE id = ?", Boolean.class, id));
    }

    private boolean existe(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE id = ?",
                Integer.class, id) == 1;
    }
}
