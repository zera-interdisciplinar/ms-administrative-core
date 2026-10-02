package com.zera.ms_administrative_core.core.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha de {@code bi.vw_alertas_mensal_unidade}.
 *
 * <p>{@code totalAcumulado}, {@code rankingNoMes} e {@code variacaoMom} vem de window functions:
 * dependem das OUTRAS linhas do resultado e por isso nao poderiam ser calculados linha a linha no
 * Java sem trazer a serie inteira para a memoria.
 */
public record MonthlyUnitAlerts(
        LocalDate mes,
        String anoMes,
        UUID unidadeId,
        String unidade,
        int totalAlertas,
        int pesoTotal,
        int alertasAbertos,
        BigDecimal mediaHorasFechamento,
        long totalAcumulado,
        int rankingNoMes,
        BigDecimal mediaMovel3m,
        int variacaoMom,
        BigDecimal variacaoMomPercentual) {}
