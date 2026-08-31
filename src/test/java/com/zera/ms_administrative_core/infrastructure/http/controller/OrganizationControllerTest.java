package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.usecase.organization.activateOrganization.ActivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail.ChangeOrganizationEmail;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationPlan.ChangeOrganizationPlan;
import com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization.DeactivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindAllOrganizations;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationByCnpj;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationByEmail;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationById;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.OrganizationOutput;
import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganizationOutput;
import com.zera.ms_administrative_core.core.usecase.organization.renameOrganization.RenameOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.suspendOrganization.SuspendOrganization;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterOrganizationRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private FindAllOrganizations findAllOrganizations;
    @MockitoBean private RegisterOrganization registerOrganization;
    @MockitoBean private FindOrganizationById findOrganizationById;
    @MockitoBean private FindOrganizationByCnpj findOrganizationByCnpj;
    @MockitoBean private FindOrganizationByEmail findOrganizationByEmail;
    @MockitoBean private RenameOrganization renameOrganization;
    @MockitoBean private ChangeOrganizationEmail changeOrganizationEmail;
    @MockitoBean private ChangeOrganizationPlan changeOrganizationPlan;
    @MockitoBean private ActivateOrganization activateOrganization;
    @MockitoBean private DeactivateOrganization deactivateOrganization;
    @MockitoBean private SuspendOrganization suspendOrganization;

    private final UUID id = UUID.randomUUID();

    private OrganizationOutput output() {
        return new OrganizationOutput(id, "Org", new Cnpj("11.222.333/0001-81"),
                new Email("org@email.com"), Plan.FREE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/v1/organization - should list organizations")
    void shouldFindAll() throws Exception {
        when(findAllOrganizations.execute(any(), any(), anyInt(), anyInt())).thenReturn(List.of(output()));

        mockMvc.perform(get("/api/v1/organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/organization?cnpj= - should delegate to findByCnpj")
    void shouldFindByCnpj() throws Exception {
        when(findOrganizationByCnpj.execute("11222333000181")).thenReturn(output());

        mockMvc.perform(get("/api/v1/organization").param("cnpj", "11222333000181"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Org"));
    }

    @Test
    @DisplayName("GET /api/v1/organization?email= - should delegate to findByEmail")
    void shouldFindByEmail() throws Exception {
        when(findOrganizationByEmail.execute("org@email.com")).thenReturn(output());

        mockMvc.perform(get("/api/v1/organization").param("email", "org@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Org"));
    }

    @Test
    @DisplayName("GET /api/v1/organization/{id} - should find by ID")
    void shouldFindById() throws Exception {
        when(findOrganizationById.execute(id)).thenReturn(output());

        mockMvc.perform(get("/api/v1/organization/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/organization/{id} - should return 404 when not found")
    void shouldReturn404() throws Exception {
        when(findOrganizationById.execute(id)).thenThrow(new OrganizationNotFoundException(id));

        mockMvc.perform(get("/api/v1/organization/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/organization - should register")
    void shouldRegister() throws Exception {
        RegisterOrganizationRequest request = new RegisterOrganizationRequest(
                "Org", "11.222.333/0001-81", "org@email.com", Plan.FREE);
        when(registerOrganization.execute(any())).thenReturn(
                new RegisterOrganizationOutput(id, "Org", new Cnpj("11.222.333/0001-81"), new Email("org@email.com")));

        mockMvc.perform(post("/api/v1/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Org"));
    }

    @Test
    @DisplayName("POST /api/v1/organization - should return 400 on invalid body")
    void shouldRejectInvalidBody() throws Exception {
        RegisterOrganizationRequest request = new RegisterOrganizationRequest(
                "", "11.222.333/0001-81", "org@email.com", Plan.FREE);

        mockMvc.perform(post("/api/v1/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/rename - should rename")
    void shouldRename() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/rename", id).param("newName", "New"))
                .andExpect(status().isNoContent());

        verify(renameOrganization).execute(id, "New");
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/email - should change email")
    void shouldChangeEmail() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/email", id).param("newEmail", "new@email.com"))
                .andExpect(status().isNoContent());

        verify(changeOrganizationEmail).execute(id, "new@email.com");
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/plan - should change plan")
    void shouldChangePlan() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/plan", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\"}"))
                .andExpect(status().isNoContent());

        verify(changeOrganizationPlan).execute(id, Plan.PRO);
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/activate - should activate")
    void shouldActivate() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/activate", id))
                .andExpect(status().isNoContent());

        verify(activateOrganization).execute(id);
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/deactivate - should deactivate")
    void shouldDeactivate() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/deactivate", id))
                .andExpect(status().isNoContent());

        verify(deactivateOrganization).execute(id);
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/suspend - should suspend")
    void shouldSuspend() throws Exception {
        mockMvc.perform(patch("/api/v1/organization/{id}/suspend", id))
                .andExpect(status().isNoContent());

        verify(suspendOrganization).execute(id);
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/{id}/suspend - should map domain exception to 404")
    void shouldMapExceptionOnSuspend() throws Exception {
        doThrow(new OrganizationNotFoundException(id)).when(suspendOrganization).execute(eq(id));

        mockMvc.perform(patch("/api/v1/organization/{id}/suspend", id))
                .andExpect(status().isNotFound());
    }
}
