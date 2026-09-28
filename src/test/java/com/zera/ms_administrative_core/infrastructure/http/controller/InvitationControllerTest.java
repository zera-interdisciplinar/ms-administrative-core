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

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationExpiredException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.usecase.user.findInvitation.FindPendingInvitations;
import com.zera.ms_administrative_core.core.usecase.user.findInvitation.PendingInvitationOutput;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCode;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCodeOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode.RegisterWithInvitationCode;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
class InvitationControllerTest {

    private static final String BASE_URL = "/api/v1/invitations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateInvitationCode generateInvitationCode;

    @MockitoBean
    private RegisterWithInvitationCode registerWithInvitationCode;

    @MockitoBean
    private FindPendingInvitations findPendingInvitations;

    // --- POST /api/v1/invitations ---

    @Test
    @DisplayName("POST /invitations - deve retornar 201 ao gerar código")
    void shouldReturn201WhenCodeGenerated() throws Exception {
        UUID managerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        GenerateInvitationCodeOutput output = new GenerateInvitationCodeOutput(
                UUID.randomUUID(), "123456", managerId, unitId, "Carol", LocalDateTime.now().plusHours(24));
        when(generateInvitationCode.execute(managerId, "Carol")).thenReturn(output);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\", \"inviteeName\": \"Carol\"}".formatted(managerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("123456"))
                .andExpect(jsonPath("$.managerId").value(managerId.toString()))
                .andExpect(jsonPath("$.inviteeName").value("Carol"));
    }

    @Test
    @DisplayName("POST /invitations - deve retornar 404 quando gestor não existe")
    void shouldReturn404WhenManagerNotFound() throws Exception {
        UUID managerId = UUID.randomUUID();
        when(generateInvitationCode.execute(managerId, "Carol")).thenThrow(new UserNotFoundException(managerId));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\", \"inviteeName\": \"Carol\"}".formatted(managerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /invitations - deve retornar 400 quando body inválido")
    void shouldReturn400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /invitations - deve retornar 400 quando inviteeName está em branco")
    void shouldReturn400WhenInviteeNameBlank() throws Exception {
        UUID managerId = UUID.randomUUID();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\", \"inviteeName\": \"\"}".formatted(managerId)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/invitations/pending ---

    @Test
    @DisplayName("GET /invitations/pending - deve listar convites pendentes do gestor")
    void shouldListPendingInvitations() throws Exception {
        UUID managerId = UUID.randomUUID();
        PendingInvitationOutput output = new PendingInvitationOutput(
                "120443", "Operadora Carol the Best", LocalDateTime.now().plusHours(23));
        when(findPendingInvitations.execute(managerId)).thenReturn(List.of(output));

        mockMvc.perform(get(BASE_URL + "/pending").param("managerId", managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("120443"))
                .andExpect(jsonPath("$[0].inviteeName").value("Operadora Carol the Best"));
    }

    @Test
    @DisplayName("GET /invitations/pending - deve retornar lista vazia quando não há convites pendentes")
    void shouldReturnEmptyListWhenNoPendingInvitations() throws Exception {
        UUID managerId = UUID.randomUUID();
        when(findPendingInvitations.execute(managerId)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/pending").param("managerId", managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /invitations/pending - deve retornar 400 quando managerId está ausente")
    void shouldReturn400WhenManagerIdMissing() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pending"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/v1/invitations/redeem ---

    @Test
    @DisplayName("POST /invitations/redeem - deve retornar 201 ao registrar funcionário")
    void shouldReturn201WhenEmployeeRegistered() throws Exception {
        RegisterUserOutput output = new RegisterUserOutput(
                UUID.randomUUID(), "Ana", new Email("ana@empresa.com"), Role.EMPLOYEE, UUID.randomUUID());
        when(registerWithInvitationCode.execute(any())).thenReturn(output);

        String body = """
                {
                  "code": "123456",
                  "name": "Ana",
                  "email": "ana@empresa.com",
                  "rawPassword": "Senha123"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Ana"));
    }

    @Test
    @DisplayName("POST /invitations/redeem - deve retornar 404 quando código não existe")
    void shouldReturn404WhenInvitationNotFound() throws Exception {
        when(registerWithInvitationCode.execute(any())).thenThrow(new InvitationNotFoundException("999999"));

        String body = """
                {
                  "code": "999999",
                  "name": "Ana",
                  "email": "ana@empresa.com",
                  "rawPassword": "Senha123"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /invitations/redeem - deve retornar 422 quando código expirou")
    void shouldReturn422WhenInvitationExpired() throws Exception {
        when(registerWithInvitationCode.execute(any())).thenThrow(new InvitationExpiredException("123456"));

        String body = """
                {
                  "code": "123456",
                  "name": "Ana",
                  "email": "ana@empresa.com",
                  "rawPassword": "Senha123"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /invitations/redeem - deve retornar 409 quando email já está em uso")
    void shouldReturn409WhenEmailAlreadyInUse() throws Exception {
        when(registerWithInvitationCode.execute(any()))
                .thenThrow(new EmailAlreadyInUseException(new Email("ana@empresa.com")));

        String body = """
                {
                  "code": "123456",
                  "name": "Ana",
                  "email": "ana@empresa.com",
                  "rawPassword": "Senha123"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /invitations/redeem - deve retornar 400 quando código não tem 6 dígitos")
    void shouldReturn400WhenCodeIsInvalid() throws Exception {
        String body = """
                {
                  "code": "12",
                  "name": "Ana",
                  "email": "ana@empresa.com",
                  "rawPassword": "Senha123"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
