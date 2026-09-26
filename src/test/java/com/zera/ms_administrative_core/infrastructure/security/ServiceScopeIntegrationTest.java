package com.zera.ms_administrative_core.infrastructure.security;

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

import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceite da ZERA-266: a rota interna so aceita token de servico. Usa STORAGE porque
 * o teste e sobre autorizacao, e assim ele nao depende dos tipos novos da ZERA-265.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceScopeIntegrationTest {

    private static final String ALERTA = """
            {"userId":"%s","unitId":"%s","description":"estoque quase cheio","severity":"MEDIUM",
             "kind":"STORAGE","status":"OPEN"}
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotifyUser notifyUser;

    private static MockHttpServletRequestBuilder withScope(MockHttpServletRequestBuilder request,
            String scope) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + scope))
                .jwt(builder -> builder.subject("ms-inventory").claim("scope", scope)));
    }

    private static MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request, String role) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(builder -> builder.subject(UUID.randomUUID().toString()).claim("role", role)));
    }

    private static MockHttpServletRequestBuilder alerta(MockHttpServletRequestBuilder request) {
        return request.contentType("application/json").content(ALERTA);
    }

    @Test
    @DisplayName("Sem token nenhum a rota interna responde 401")
    void shouldRejectWithoutAToken() throws Exception {
        mockMvc.perform(alerta(post("/api/v1/notifications/alerts")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(notifyUser);
    }

    /** Antes bastava um JWT valido qualquer; agora o papel de usuario nao alcanca a rota. */
    @Test
    @DisplayName("Token de gestor nao acessa a rota interna")
    void shouldRejectAManagerToken() throws Exception {
        mockMvc.perform(alerta(asRole(post("/api/v1/notifications/alerts"), "MANAGER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notifyUser);
    }

    @Test
    @DisplayName("Token de operario nao acessa a rota interna")
    void shouldRejectAnEmployeeToken() throws Exception {
        mockMvc.perform(alerta(asRole(post("/api/v1/notifications/alerts"), "EMPLOYEE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notifyUser);
    }

    @Test
    @DisplayName("Escopo de servico errado nao acessa a rota interna")
    void shouldRejectTheWrongScope() throws Exception {
        mockMvc.perform(alerta(withScope(post("/api/v1/notifications/alerts"), "outro:escopo")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notifyUser);
    }

    @Test
    @DisplayName("Token de servico com o escopo dedicado envia o alerta")
    void shouldAcceptTheServiceScope() throws Exception {
        mockMvc.perform(alerta(withScope(post("/api/v1/notifications/alerts"), "notifications:write")))
                .andExpect(status().isAccepted());
    }

    /** O endpoint que emite o token de servico e publico: e por ele que o cliente se autentica. */
    @Test
    @DisplayName("O endpoint de token de servico nao exige autenticacao")
    void shouldLeaveTheTokenEndpointPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/service-token")
                        .contentType("application/json")
                        .content("{\"clientId\":\"ms-inventory\",\"clientSecret\":\"errado\"}"))
                .andExpect(status().isUnauthorized());
    }
}
