package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.security.Principal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;
import com.zera.ms_administrative_core.core.usecase.devicetoken.registerDeviceToken.RegisterDeviceToken;
import com.zera.ms_administrative_core.core.usecase.devicetoken.unregisterDeviceToken.UnregisterDeviceToken;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterDeviceTokenRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.UnregisterDeviceTokenRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceTokenController.class)
class DeviceTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterDeviceToken registerDeviceToken;

    @MockitoBean
    private UnregisterDeviceToken unregisterDeviceToken;

    @Test
    @DisplayName("POST /api/v1/users/me/device-tokens - deve registrar o token do usuario do token")
    void shouldRegisterDeviceToken() throws Exception {
        UUID userId = UUID.randomUUID();
        Principal principal = userId::toString;
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("token-abc", DevicePlatform.ANDROID);

        mockMvc.perform(post("/api/v1/users/me/device-tokens")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(registerDeviceToken).execute(userId, "token-abc", DevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("POST /api/v1/users/me/device-tokens - corpo incompleto deve retornar 400")
    void shouldRejectIncompletePayload() throws Exception {
        UUID userId = UUID.randomUUID();
        Principal principal = userId::toString;

        mockMvc.perform(post("/api/v1/users/me/device-tokens")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registerDeviceToken);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me/device-tokens - deve desregistrar o token do usuario do token")
    void shouldUnregisterDeviceToken() throws Exception {
        UUID userId = UUID.randomUUID();
        Principal principal = userId::toString;
        UnregisterDeviceTokenRequest request = new UnregisterDeviceTokenRequest("token-abc");

        mockMvc.perform(delete("/api/v1/users/me/device-tokens")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(unregisterDeviceToken).execute(userId, "token-abc");
    }
}
