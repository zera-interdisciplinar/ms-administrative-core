package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;
import com.zera.ms_administrative_core.infrastructure.security.Authz;

/**
 * Acionamento manual das procedures de manutencao.
 *
 * <p>Sao POST e nao GET porque MUDAM DADO -- inclusive em massa. Um GET aqui seria pre-buscado por
 * navegador, crawler ou proxy, e uma varredura de link fecharia alertas em producao.
 *
 * <p><b>PENDENCIA DE PRODUTO -- ESCOPO MULTI-TENANT.</b> As procedures acionadas aqui operam sobre o
 * banco INTEIRO: {@code sp_fechar_alertas_obsoletos} nao filtra por organizacao nem por unidade.
 * Como a autorizacao e {@code hasRole('MANAGER')}, um gestor da organizacao A fecha, hoje, alertas
 * abertos da organizacao B -- e alerta fechado some da tela do gestor de B.
 *
 * <p>Enquanto o produto e mono-organizacao isso e inofensivo, e e a mesma pendencia que o
 * {@code AnalyticsController} ja registra para LEITURA. A diferenca e que aqui e ESCRITA EM MASSA,
 * entao a decisao e mais cara: no momento em que existirem duas organizacoes reais, ou estas rotas
 * passam a escopar pela organizacao do token, ou deixam de aceitar {@code MANAGER} e passam a exigir
 * apenas {@code SCOPE_maintenance:write} (operacao de plataforma, nao de usuario).
 *
 * <p>Nao foi decidido aqui de proposito: e decisao de produto, e o AGENTS.md pede para perguntar
 * antes de supor quando a mudanca afeta o fluxo de alertas.
 */
@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final CloseStaleAlerts closeStaleAlerts;
    private final RevokeExpiredTokens revokeExpiredTokens;
    private final ConsolidateDau consolidateDau;

    public MaintenanceController(CloseStaleAlerts closeStaleAlerts,
                                 RevokeExpiredTokens revokeExpiredTokens,
                                 ConsolidateDau consolidateDau) {
        this.closeStaleAlerts = closeStaleAlerts;
        this.revokeExpiredTokens = revokeExpiredTokens;
        this.consolidateDau = consolidateDau;
    }

    /** Aciona {@code sp_fechar_alertas_obsoletos}. */
    @PostMapping("/alerts/close-stale")
    @PreAuthorize(Authz.MANAGER_OR_SERVICE_MAINTENANCE)
    public ResponseEntity<Map<String, Integer>> closeStale(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(Map.of("fechados", closeStaleAlerts.execute(dias)));
    }

    /** Aciona {@code sp_revogar_tokens_expirados}. */
    @PostMapping("/tokens/revoke-expired")
    @PreAuthorize(Authz.MANAGER_OR_SERVICE_MAINTENANCE)
    public ResponseEntity<TokenCleanup> revokeExpired(
            @RequestParam(defaultValue = "7") int diasRetencao) {
        return ResponseEntity.ok(revokeExpiredTokens.execute(diasRetencao));
    }

    /** Aciona {@code sp_consolidar_dau}: reconstroi o rollup a partir do log de acessos. */
    @PostMapping("/dau/consolidate")
    @PreAuthorize(Authz.MANAGER_OR_SERVICE_MAINTENANCE)
    public ResponseEntity<Map<String, Integer>> consolidate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(Map.of("dias", consolidateDau.execute(de, ate)));
    }
}
