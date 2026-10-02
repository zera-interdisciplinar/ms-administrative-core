package com.zera.ms_administrative_core.infrastructure.http.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;

@WebMvcTest(MaintenanceController.class)
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CloseStaleAlerts closeStaleAlerts;
    @MockitoBean private RevokeExpiredTokens revokeExpiredTokens;
    @MockitoBean private ConsolidateDau consolidateDau;

    @Test
    @DisplayName("POST /maintenance/alerts/close-stale - deve acionar a procedure e devolver a contagem")
    void shouldTriggerCloseStale() throws Exception {
        when(closeStaleAlerts.execute(45)).thenReturn(7);

        mockMvc.perform(post("/api/v1/maintenance/alerts/close-stale").param("dias", "45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechados").value(7));

        verify(closeStaleAlerts).execute(45);
    }

    @Test
    @DisplayName("POST /maintenance/alerts/close-stale - deve usar 30 dias como padrao")
    void shouldDefaultToThirtyDays() throws Exception {
        when(closeStaleAlerts.execute(30)).thenReturn(0);

        mockMvc.perform(post("/api/v1/maintenance/alerts/close-stale"))
                .andExpect(status().isOk());

        verify(closeStaleAlerts).execute(30);
    }

    @Test
    @DisplayName("POST /maintenance/tokens/revoke-expired - deve devolver revogados e removidos")
    void shouldTriggerTokenCleanup() throws Exception {
        when(revokeExpiredTokens.execute(7)).thenReturn(new TokenCleanup(5, 2));

        mockMvc.perform(post("/api/v1/maintenance/tokens/revoke-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revogados").value(5))
                .andExpect(jsonPath("$.removidos").value(2));
    }

    @Test
    @DisplayName("POST /maintenance/dau/consolidate - deve repassar o periodo")
    void shouldTriggerDauConsolidation() throws Exception {
        when(consolidateDau.execute(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(31);

        mockMvc.perform(post("/api/v1/maintenance/dau/consolidate")
                        .param("de", "2026-01-01").param("ate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias").value(31));
    }

    /** Janela abaixo do piso e 400, tratado pelo handler global de IllegalArgumentException. */
    @Test
    @DisplayName("POST /maintenance/alerts/close-stale - janela invalida deve dar 400")
    void shouldReturnBadRequestForInvalidWindow() throws Exception {
        when(closeStaleAlerts.execute(2)).thenThrow(new IllegalArgumentException("minimo 7 dias"));

        mockMvc.perform(post("/api/v1/maintenance/alerts/close-stale").param("dias", "2"))
                .andExpect(status().isBadRequest());
    }
}
