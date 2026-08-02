package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.time.LocalDateTime;
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
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotifyUser notifyUser;

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
}
