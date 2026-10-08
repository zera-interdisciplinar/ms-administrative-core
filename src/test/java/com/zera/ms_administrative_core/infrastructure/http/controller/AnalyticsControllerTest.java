package com.zera.ms_administrative_core.infrastructure.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRanking;
import com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers.FindDailyActiveUsers;
import com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts.FindMonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.usecase.analytics.findTeamSize.FindTeamSize;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth.FindUnitHealth;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking.FindUnitRanking;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FindMonthlyUnitAlerts findMonthlyUnitAlerts;
    @MockitoBean private FindUnitRanking findUnitRanking;
    @MockitoBean private FindDailyActiveUsers findDailyActiveUsers;
    @MockitoBean private FindUnitHealth findUnitHealth;
    @MockitoBean private FindTeamSize findTeamSize;

    private final UUID unidadeId = UUID.randomUUID();

    @Test
    @DisplayName("GET /analytics/units/{id}/alerts/monthly - deve expor os calculos de janela")
    void shouldReturnMonthlySeries() throws Exception {
        when(findMonthlyUnitAlerts.execute(unidadeId)).thenReturn(List.of(new MonthlyUnitAlerts(
                LocalDate.of(2026, 3, 1), "2026-03", unidadeId, "Unidade A", 10, 30, 2,
                BigDecimal.valueOf(4.5), 45L, 2, BigDecimal.valueOf(9.33), 3,
                BigDecimal.valueOf(42.86))));

        mockMvc.perform(get("/api/v1/analytics/units/{id}/alerts/monthly", unidadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anoMes").value("2026-03"))
                .andExpect(jsonPath("$[0].totalAcumulado").value(45))
                .andExpect(jsonPath("$[0].rankingNoMes").value(2))
                .andExpect(jsonPath("$[0].variacaoMom").value(3));
    }

    @Test
    @DisplayName("GET /analytics/units/{id}/alerts/monthly - unidade inexistente deve dar 404")
    void shouldReturnNotFoundForUnknownUnit() throws Exception {
        when(findMonthlyUnitAlerts.execute(unidadeId)).thenThrow(new UnitNotFoundException(unidadeId));

        mockMvc.perform(get("/api/v1/analytics/units/{id}/alerts/monthly", unidadeId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /analytics/units/ranking - deve expor quartil e percentil")
    void shouldReturnRanking() throws Exception {
        when(findUnitRanking.execute(20)).thenReturn(List.of(new UnitRanking(
                unidadeId, "Unidade A", "Santos", "SP", 99, 297, 12, 8,
                BigDecimal.valueOf(37.13), 1, 1, BigDecimal.valueOf(100), BigDecimal.valueOf(18.4))));

        mockMvc.perform(get("/api/v1/analytics/units/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rankingGeral").value(1))
                .andExpect(jsonPath("$[0].quartilGravidade").value(1));

        verify(findUnitRanking).execute(20);
    }

    @Test
    @DisplayName("GET /analytics/dau - deve exigir periodo e expor a media movel")
    void shouldReturnDau() throws Exception {
        LocalDate de = LocalDate.of(2026, 1, 1);
        when(findDailyActiveUsers.execute(de, de.plusDays(6))).thenReturn(List.of(
                new DailyActiveUsers(de, 15, 22, BigDecimal.valueOf(12.14), 150L, -2, 40,
                        BigDecimal.valueOf(37.5))));

        mockMvc.perform(get("/api/v1/analytics/dau")
                        .param("de", "2026-01-01").param("ate", "2026-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dau").value(15))
                .andExpect(jsonPath("$[0].mediaMovel7d").value(12.14))
                .andExpect(jsonPath("$[0].aderenciaDauWau").value(37.5));
    }

    @Test
    @DisplayName("GET /analytics/dau - sem periodo deve dar 400")
    void shouldRequirePeriod() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dau"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /analytics/units/{id}/health - deve devolver o indice da function")
    void shouldReturnHealthIndex() throws Exception {
        when(findUnitHealth.execute(any(), any(), any())).thenReturn(BigDecimal.valueOf(64.28));

        mockMvc.perform(get("/api/v1/analytics/units/{id}/health", unidadeId)
                        .param("de", "2026-01-01").param("ate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indiceSaude").value(64.28))
                .andExpect(jsonPath("$.unidadeId").value(unidadeId.toString()));
    }

    @Test
    @DisplayName("GET /analytics/managers/{id}/team-size - deve devolver o tamanho da equipe")
    void shouldReturnTeamSize() throws Exception {
        when(findTeamSize.execute(unidadeId)).thenReturn(7);

        mockMvc.perform(get("/api/v1/analytics/managers/{id}/team-size", unidadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gestorId").value(unidadeId.toString()))
                .andExpect(jsonPath("$.tamanhoEquipe").value(7));
    }
}
