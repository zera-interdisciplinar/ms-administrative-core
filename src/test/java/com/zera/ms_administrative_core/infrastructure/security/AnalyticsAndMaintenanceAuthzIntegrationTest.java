package com.zera.ms_administrative_core.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers.FindDailyActiveUsers;
import com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts.FindMonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth.FindUnitHealth;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking.FindUnitRanking;
import com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog.FindDataCatalog;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;

/**
 * Autorizacao das rotas de analytics, manutencao e governanca, com a cadeia de seguranca REAL.
 *
 * <p>Os {@code @WebMvcTest} dos controllers rodam sem seguranca (convencao do projeto), entao la um
 * {@code @PreAuthorize} esquecido -- ou um typo em {@code SCOPE_maintenance:write} -- passaria a
 * suite inteira em verde. Para rotas que mudam milhares de linhas de uma vez, esse e o teste que
 * importa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsAndMaintenanceAuthzIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CloseStaleAlerts closeStaleAlerts;
    @MockitoBean private RevokeExpiredTokens revokeExpiredTokens;
    @MockitoBean private ConsolidateDau consolidateDau;
    @MockitoBean private FindMonthlyUnitAlerts findMonthlyUnitAlerts;
    @MockitoBean private FindUnitRanking findUnitRanking;
    @MockitoBean private FindDailyActiveUsers findDailyActiveUsers;
    @MockitoBean private FindUnitHealth findUnitHealth;
    @MockitoBean private FindDataCatalog findDataCatalog;

    private static MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request,
                                                        String role) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(b -> b.subject(UUID.randomUUID().toString()).claim("role", role)));
    }

    private static MockHttpServletRequestBuilder withScope(MockHttpServletRequestBuilder request,
                                                           String scope) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + scope))
                .jwt(b -> b.subject("ms-inventory").claim("scope", scope)));
    }

    // ------------------------------------------------------------ manutencao

    @Test
    @DisplayName("Fechar alertas em massa sem token responde 401")
    void closeStaleRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance/alerts/close-stale"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(closeStaleAlerts);
    }

    /** O teste que mais importa aqui: EMPLOYEE nao muda milhares de alertas. */
    @Test
    @DisplayName("EMPLOYEE nao pode fechar alertas em massa")
    void closeStaleForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(post("/api/v1/maintenance/alerts/close-stale"), "EMPLOYEE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(closeStaleAlerts);
    }

    @Test
    @DisplayName("MANAGER pode fechar alertas em massa")
    void closeStaleAllowedForManager() throws Exception {
        when(closeStaleAlerts.execute(anyInt())).thenReturn(3);

        mockMvc.perform(asRole(post("/api/v1/maintenance/alerts/close-stale"), "MANAGER"))
                .andExpect(status().isOk());
    }

    /** Protege o literal do escopo: um typo em Authz.MANAGER_OR_SERVICE_MAINTENANCE quebra aqui. */
    @Test
    @DisplayName("Token de servico com escopo maintenance:write e aceito")
    void maintenanceAllowedForServiceScope() throws Exception {
        when(revokeExpiredTokens.execute(anyInt())).thenReturn(new TokenCleanup(1, 0));

        mockMvc.perform(withScope(post("/api/v1/maintenance/tokens/revoke-expired"),
                        "maintenance:write"))
                .andExpect(status().isOk());
    }

    /**
     * Quem pode entregar alerta nao pode, por isso, fechar alerta em massa: os escopos sao
     * separados de proposito.
     */
    @Test
    @DisplayName("Escopo notifications:write nao alcanca a manutencao")
    void maintenanceForbiddenForNotificationsScope() throws Exception {
        mockMvc.perform(withScope(post("/api/v1/maintenance/alerts/close-stale"),
                        "notifications:write"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(closeStaleAlerts);
    }

    @Test
    @DisplayName("Consolidacao de DAU tambem exige gestor ou escopo")
    void consolidateDauForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(post("/api/v1/maintenance/dau/consolidate"), "EMPLOYEE")
                        .param("de", "2026-01-01").param("ate", "2026-01-31"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(consolidateDau);
    }

    // -------------------------------------------------------------- analytics

    @Test
    @DisplayName("EMPLOYEE nao ve o ranking comparado de unidades")
    void rankingForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(get("/api/v1/analytics/units/ranking"), "EMPLOYEE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(findUnitRanking);
    }

    @Test
    @DisplayName("MANAGER ve o ranking")
    void rankingAllowedForManager() throws Exception {
        when(findUnitRanking.execute(anyInt())).thenReturn(List.of());

        mockMvc.perform(asRole(get("/api/v1/analytics/units/ranking"), "MANAGER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE nao ve o DAU")
    void dauForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(get("/api/v1/analytics/dau"), "EMPLOYEE")
                        .param("de", "2026-01-01").param("ate", "2026-01-07"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(findDailyActiveUsers);
    }

    @Test
    @DisplayName("EMPLOYEE nao ve a serie mensal de uma unidade")
    void monthlyForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(get("/api/v1/analytics/units/{id}/alerts/monthly",
                        UUID.randomUUID()), "EMPLOYEE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(findMonthlyUnitAlerts);
    }

    @Test
    @DisplayName("MANAGER ve o indice de saude da unidade")
    void healthAllowedForManager() throws Exception {
        when(findUnitHealth.execute(any(), any(), any())).thenReturn(BigDecimal.valueOf(80));

        mockMvc.perform(asRole(get("/api/v1/analytics/units/{id}/health", UUID.randomUUID()),
                        "MANAGER")
                        .param("de", "2026-01-01").param("ate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ governanca

    @Test
    @DisplayName("EMPLOYEE nao le o catalogo de dados")
    void catalogForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(get("/api/v1/governance/data-catalog"), "EMPLOYEE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(findDataCatalog);
    }

    @Test
    @DisplayName("MANAGER le o catalogo e a divergencia")
    void catalogAllowedForManager() throws Exception {
        when(findDataCatalog.tables()).thenReturn(List.of());
        when(findDataCatalog.drift()).thenReturn(List.of());

        mockMvc.perform(asRole(get("/api/v1/governance/data-catalog"), "MANAGER"))
                .andExpect(status().isOk());
        mockMvc.perform(asRole(get("/api/v1/governance/data-catalog/drift"), "MANAGER"))
                .andExpect(status().isOk());
    }
}
