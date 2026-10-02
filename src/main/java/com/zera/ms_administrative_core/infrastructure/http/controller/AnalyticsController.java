package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRanking;
import com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers.FindDailyActiveUsers;
import com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts.FindMonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth.FindUnitHealth;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking.FindUnitRanking;
import com.zera.ms_administrative_core.infrastructure.security.Authz;

/**
 * Camada analitica exposta por HTTP: le as views dimensionais e a function de saude da unidade.
 *
 * <p>Todas as rotas exigem {@code MANAGER}. Sao numeros agregados de MULTIPLAS unidades, e um
 * EMPLOYEE nao tem por que ver o desempenho comparado de outras unidades da organizacao.
 *
 * <p>PENDENCIA DE PRODUTO: a v1 nao restringe um gestor as unidades da PROPRIA organizacao -- o
 * ranking sai global. Enquanto o produto e mono-organizacao isso e inofensivo; no momento em que
 * houver duas organizacoes reais, isto precisa filtrar pela organizacao do token.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final FindMonthlyUnitAlerts findMonthlyUnitAlerts;
    private final FindUnitRanking findUnitRanking;
    private final FindDailyActiveUsers findDailyActiveUsers;
    private final FindUnitHealth findUnitHealth;

    public AnalyticsController(FindMonthlyUnitAlerts findMonthlyUnitAlerts,
                               FindUnitRanking findUnitRanking,
                               FindDailyActiveUsers findDailyActiveUsers,
                               FindUnitHealth findUnitHealth) {
        this.findMonthlyUnitAlerts = findMonthlyUnitAlerts;
        this.findUnitRanking = findUnitRanking;
        this.findDailyActiveUsers = findDailyActiveUsers;
        this.findUnitHealth = findUnitHealth;
    }

    /** Serie mensal de alertas da unidade, com running total, ranking e variacao MoM. */
    @GetMapping("/units/{id}/alerts/monthly")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<MonthlyUnitAlerts>> monthlyAlerts(@PathVariable UUID id) {
        return ResponseEntity.ok(findMonthlyUnitAlerts.execute(id));
    }

    /** Ranking de unidades por gravidade nos ultimos 90 dias, com quartil e percentil. */
    @GetMapping("/units/ranking")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<UnitRanking>> ranking(@RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(findUnitRanking.execute(limite));
    }

    /** DAU diario com media movel de 7 dias e aderencia DAU/WAU. */
    @GetMapping("/dau")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<DailyActiveUsers>> dau(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(findDailyActiveUsers.execute(de, ate));
    }

    /** Indice 0-100 de saude da unidade no periodo (function no banco). */
    @GetMapping("/units/{id}/health")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<Map<String, Object>> health(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        // HashMap e nao Map.of: Map.of estoura NullPointerException com valor nulo, e um 500 por
        // indice ausente seria pior que devolver null explicito ao cliente.
        BigDecimal indice = findUnitHealth.execute(id, de, ate);
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("unidadeId", id);
        corpo.put("de", de);
        corpo.put("ate", ate);
        corpo.put("indiceSaude", indice);
        return ResponseEntity.ok(corpo);
    }
}
