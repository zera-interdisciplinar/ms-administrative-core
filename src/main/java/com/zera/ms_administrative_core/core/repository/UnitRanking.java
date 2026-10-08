package com.zera.ms_administrative_core.core.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Uma linha de {@code bi.vw_ranking_unidades}. */
public record UnitRanking(
        UUID unidadeId,
        String unidade,
        String cidade,
        String estado,
        int totalAlertas,
        int pesoTotal,
        int alertasAbertos,
        int usuariosAtivos,
        BigDecimal pesoPorUsuario,
        int rankingGeral,
        int quartilGravidade,
        BigDecimal percentil,
        BigDecimal participacaoPercentual) {}
