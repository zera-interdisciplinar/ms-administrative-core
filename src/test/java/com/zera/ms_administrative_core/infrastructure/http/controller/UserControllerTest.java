package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.exception.InvalidStatusTransitionException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.activateUser.ActivateUser;
import com.zera.ms_administrative_core.core.usecase.user.assignManager.AssignManager;
import com.zera.ms_administrative_core.core.usecase.user.changeUserEmail.ChangeEmail;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePassword;
import com.zera.ms_administrative_core.core.usecase.user.deactivateUser.DeactivateUser;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindAllUsers;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindUserByEmail;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindUserById;
import com.zera.ms_administrative_core.core.usecase.user.findUser.UserOutput;
import com.zera.ms_administrative_core.core.usecase.user.renameUser.RenameUser;
import com.zera.ms_administrative_core.core.usecase.user.suspendUser.SuspendUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private FindUserById findUserById;
    @MockitoBean private FindUserByEmail findUserByEmail;
    @MockitoBean private FindAllUsers findAllUsers;
    @MockitoBean private RenameUser renameUser;
    @MockitoBean private ChangeEmail changeEmail;
    @MockitoBean private ChangePassword changePassword;
    @MockitoBean private ActivateUser activateUser;
    @MockitoBean private DeactivateUser deactivateUser;
    @MockitoBean private SuspendUser suspendUser;
    @MockitoBean private AssignManager assignManager;

    private UserOutput userOutput;

    @BeforeEach
    void setUp() {
        userOutput = new UserOutput(
                UUID.randomUUID(),
                "João Silva",
                new Email("joao@empresa.com"),
                Role.MANAGER,
                Status.ACTIVE,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    // --- GET /api/v1/users/{id} ---

    @Test
    @DisplayName("GET /users/{id} - deve retornar 200 quando encontrado")
    void shouldReturn200WhenUserFound() throws Exception {
        when(findUserById.execute(any())).thenReturn(userOutput);

        mockMvc.perform(get(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /users/{id} - deve retornar 404 quando não encontrado")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(findUserById.execute(any())).thenThrow(new UserNotFoundException(UUID.randomUUID()));

        mockMvc.perform(get(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/v1/users ---

    @Test
    @DisplayName("GET /users - deve retornar 200 com lista paginada")
    void shouldReturn200WithPagedList() throws Exception {
        when(findAllUsers.execute(null, null, 0, 20)).thenReturn(List.of(userOutput));

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("João Silva"));
    }

    @Test
    @DisplayName("GET /users?role=MANAGER - deve filtrar por role")
    void shouldReturn200FilteredByRole() throws Exception {
        when(findAllUsers.execute(eq(Role.MANAGER), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(userOutput));

        mockMvc.perform(get(BASE_URL).param("role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
    }

    @Test
    @DisplayName("GET /users?status=ACTIVE - deve filtrar por status")
    void shouldReturn200FilteredByStatus() throws Exception {
        when(findAllUsers.execute(isNull(), eq(Status.ACTIVE), anyInt(), anyInt()))
                .thenReturn(List.of(userOutput));

        mockMvc.perform(get(BASE_URL).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /users?email= - deve buscar por email")
    void shouldReturn200WhenSearchByEmail() throws Exception {
        when(findUserByEmail.execute("joao@empresa.com")).thenReturn(userOutput);

        mockMvc.perform(get(BASE_URL).param("email", "joao@empresa.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("joao@empresa.com"));
    }

    @Test
    @DisplayName("GET /users?email= - deve retornar 404 quando email não encontrado")
    void shouldReturn404WhenEmailNotFound() throws Exception {
        when(findUserByEmail.execute(any()))
                .thenThrow(new UserNotFoundException(new Email("naoexiste@empresa.com")));

        mockMvc.perform(get(BASE_URL).param("email", "naoexiste@empresa.com"))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/v1/users/{id}/activate ---

    @Test
    @DisplayName("PATCH /users/{id}/activate - deve retornar 204")
    void shouldReturn204WhenActivated() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/activate", UUID.randomUUID()))
                .andExpect(status().isNoContent());
        verify(activateUser).execute(any());
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate - deve retornar 422 em transição inválida")
    void shouldReturn422WhenInvalidTransition() throws Exception {
        doThrow(new InvalidStatusTransitionException(Status.ACTIVE, Status.ACTIVE))
                .when(activateUser).execute(any());

        mockMvc.perform(patch(BASE_URL + "/{id}/activate", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- PATCH /api/v1/users/{id}/suspend ---

    @Test
    @DisplayName("PATCH /users/{id}/suspend - deve retornar 204")
    void shouldReturn204WhenSuspended() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/suspend", UUID.randomUUID()))
                .andExpect(status().isNoContent());
        verify(suspendUser).execute(any());
    }

    // --- PATCH /api/v1/users/{id}/deactivate ---

    @Test
    @DisplayName("PATCH /users/{id}/deactivate - deve retornar 204")
    void shouldReturn204WhenDeactivated() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", UUID.randomUUID()))
                .andExpect(status().isNoContent());
        verify(deactivateUser).execute(any());
    }

    // --- PATCH /api/v1/users/{id}/manager ---

    @Test
    @DisplayName("PATCH /users/{id}/manager - deve retornar 204 ao atribuir gestor")
    void shouldReturn204WhenManagerAssigned() throws Exception {
        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        mockMvc.perform(patch(BASE_URL + "/{id}/manager", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\"}".formatted(managerId)))
                .andExpect(status().isNoContent());
        verify(assignManager).execute(id, managerId);
    }

    @Test
    @DisplayName("PATCH /users/{id}/manager - deve retornar 204 ao remover gestor")
    void shouldReturn204WhenManagerUnassigned() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch(BASE_URL + "/{id}/manager", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
        verify(assignManager).execute(id, null);
    }

    @Test
    @DisplayName("PATCH /users/{id}/manager - deve retornar 404 quando usuário não encontrado")
    void shouldReturn404WhenUserNotFoundOnAssignManager() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new UserNotFoundException(id)).when(assignManager).execute(eq(id), any());

        mockMvc.perform(patch(BASE_URL + "/{id}/manager", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /users/{id}/manager - deve retornar 400 quando alvo não é funcionário")
    void shouldReturn400WhenTargetIsNotEmployeeOnAssignManager() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Only employees can have a manager assigned"))
                .when(assignManager).execute(eq(id), any());

        mockMvc.perform(patch(BASE_URL + "/{id}/manager", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}