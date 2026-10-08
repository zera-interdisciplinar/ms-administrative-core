package com.zera.ms_administrative_core.core.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha de {@code bi.vw_dau_diario}. */
public record DailyActiveUsers(
        LocalDate dia,
        int dau,
        int acessos,
        BigDecimal mediaMovel7d,
        long acessosAcumulados,
        int variacaoDod,
        int usuariosUnicos7d,
        BigDecimal aderenciaDauWau) {}
