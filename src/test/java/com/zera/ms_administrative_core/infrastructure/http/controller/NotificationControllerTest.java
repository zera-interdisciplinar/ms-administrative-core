package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.AlertOutput;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.FindAlertsForUser;
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotifyUser notifyUser;

    @MockitoBean
    private FindAlertsForUser findAlertsForUser;

    @Test
    @DisplayName("POST /api/v1/notifications/alerts - deve aceitar o alerta e chamar o use case")
    void shouldAcceptAlertNotification() throws Exception {
        AlertNotificationRequest request = new AlertNotificationRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(notifyUser).execute(request.toCommand());
    }

    /** Todo tipo novo do ms-inventory v1 precisa ser aceito e chegar ao use case. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(AlertKind.class)
    @DisplayName("POST /api/v1/notifications/alerts - deve aceitar todos os tipos de alerta")
    void shouldAcceptEveryAlertKind(AlertKind kind) throws Exception {
        AlertNotificationRequest request = new AlertNotificationRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "alerta de " + kind, Severity.MEDIUM, kind, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(notifyUser).execute(request.toCommand());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/alerts - tipo desconhecido deve retornar 400")
    void shouldRejectAnUnknownKind() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s","unitId":"%s","description":"x","severity":"HIGH",
                         "kind":"NAO_EXISTE","status":"OPEN"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(notifyUser);
    }

    /**
     * Antes nada era validado e um corpo incompleto so falhava adiante, como 404 ou 500. O alerta
     * vem de outro servico, entao a borda precisa recusar cedo e dizer o que faltou.
     */
    @Test
    @DisplayName("POST /api/v1/notifications/alerts - corpo incompleto deve retornar 400")
    void shouldRejectAnIncompletePayload() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s","unitId":"%s","description":"  ","severity":"HIGH",
                         "kind":"STORAGE","status":"OPEN"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(notifyUser);
    }

    @Test
    @DisplayName("POST /api/v1/notifications/alerts - deve recusar momento no futuro")
    void shouldRejectAFutureMoment() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s","unitId":"%s","description":"x","severity":"HIGH",
                         "kind":"STORAGE","status":"OPEN","occurredAt":"%s"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID(),
                                LocalDateTime.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    /** eventId e ruleId seguem opcionais: sem eles o alerta e criado, so nao ha deduplicacao. */
    @Test
    @DisplayName("POST /api/v1/notifications/alerts - deve aceitar sem eventId e ruleId")
    void shouldAcceptWithoutDeduplicationKeys() throws Exception {
        AlertNotificationRequest request = new AlertNotificationRequest(
                null, null, UUID.randomUUID(), UUID.randomUUID(), "sem dedup", Severity.LOW,
                AlertKind.ITEM_APPROVED, AlertStatus.OPEN, null);

        mockMvc.perform(post("/api/v1/notifications/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(notifyUser).execute(request.toCommand());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/alerts - deve listar os alertas do usuario do token")
    void shouldListAlertsForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        AlertOutput output = new AlertOutput(UUID.randomUUID(), AlertKind.STORAGE, Severity.HIGH,
                AlertStatus.OPEN, "estoque cheio", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2024, 1, 1, 8, 0), LocalDateTime.of(2024, 1, 1, 8, 0),
                LocalDateTime.of(2024, 1, 1, 8, 0));

        when(findAlertsForUser.execute(eq(userId), isNull(), eq(0), eq(20))).thenReturn(List.of(output));

        Principal principal = userId::toString;

        mockMvc.perform(get("/api/v1/notifications/alerts").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value(output.alertId().toString()));

        verify(findAlertsForUser).execute(userId, null, 0, 20);
    }

    @Test
    @DisplayName("GET /api/v1/notifications/alerts - deve repassar o filtro de status e a paginacao")
    void shouldListAlertsFilteredByStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        Principal principal = userId::toString;

        when(findAlertsForUser.execute(eq(userId), eq(AlertStatus.CLOSED), eq(1), eq(5)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications/alerts")
                        .principal(principal)
                        .param("status", "CLOSED")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(findAlertsForUser).execute(userId, AlertStatus.CLOSED, 1, 5);
    }
}
