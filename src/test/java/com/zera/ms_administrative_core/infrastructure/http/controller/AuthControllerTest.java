package com.zera.ms_administrative_core.infrastructure.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidRefreshTokenException;
import com.zera.ms_administrative_core.core.usecase.auth.Login;
import com.zera.ms_administrative_core.core.usecase.auth.Logout;
import com.zera.ms_administrative_core.core.usecase.auth.RefreshSession;
import com.zera.ms_administrative_core.core.usecase.auth.TokenPair;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String BASE_URL = "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private Login login;
    @MockitoBean private RefreshSession refreshSession;
    @MockitoBean private Logout logout;

    @Test
    @DisplayName("POST /auth/login - 200 com o par de tokens")
    void loginReturnsTokenPair() throws Exception {
        when(login.execute("alice@empresa.com", "secret"))
                .thenReturn(TokenPair.bearer("access.jwt", "refresh-opaque", 900));

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@empresa.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-opaque"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /auth/login - 401 em credenciais invalidas")
    void loginRejectsBadCredentials() throws Exception {
        when(login.execute(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@empresa.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - 400 quando falta campo")
    void loginValidatesBody() throws Exception {
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@empresa.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/refresh - 200 com novo par")
    void refreshReturnsNewPair() throws Exception {
        when(refreshSession.execute("refresh-opaque"))
                .thenReturn(TokenPair.bearer("access2.jwt", "refresh2", 900));

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-opaque\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access2.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh2"));
    }

    @Test
    @DisplayName("POST /auth/refresh - 401 quando token invalido")
    void refreshRejectsInvalidToken() throws Exception {
        when(refreshSession.execute(any())).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout - 204 e delega para o use case")
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-opaque\"}"))
                .andExpect(status().isNoContent());
        verify(logout).execute("refresh-opaque");
    }

    @Test
    @DisplayName("POST /auth/logout - 400 quando body vazio")
    void logoutValidatesBody() throws Exception {
        doThrow(new AssertionError("nao deveria chamar")).when(logout).execute(any());

        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
