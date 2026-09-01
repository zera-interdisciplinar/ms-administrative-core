package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.usecase.telephone.changeTelephone.ChangeTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.deleteTelephone.DeleteTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindAllTelephones;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindAllTelephonesByOrganizationId;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneById;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneByRecyclingBusinessId;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneByUserId;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.TelephoneOutput;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterRecyclingTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterTelephoneOutput;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterUserTelephone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelephoneController.class)
class TelephoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ChangeTelephone changeTelephone;
    @MockitoBean private DeleteTelephone deleteTelephone;
    @MockitoBean private FindAllTelephones findAllTelephones;
    @MockitoBean private FindAllTelephonesByOrganizationId findAllTelephonesByOrganizationId;
    @MockitoBean private FindTelephoneById findTelephoneById;
    @MockitoBean private FindTelephoneByRecyclingBusinessId findTelephoneByRecyclingBusinessId;
    @MockitoBean private FindTelephoneByUserId findTelephoneByUserId;
    @MockitoBean private RegisterRecyclingTelephone registerRecyclingTelephone;
    @MockitoBean private RegisterUserTelephone registerUserTelephone;

    private final UUID id = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final UUID recyclingId = UUID.randomUUID();

    private TelephoneOutput output() {
        return new TelephoneOutput(id, new TelephoneNumber("11987654321"), userId, organizationId,
                UUID.randomUUID(), null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/v1/telephone - should list telephones")
    void shouldFindAll() throws Exception {
        when(findAllTelephones.execute(anyInt(), anyInt())).thenReturn(List.of(output()));

        mockMvc.perform(get("/api/v1/telephone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telephoneId").value(id.toString()))
                .andExpect(jsonPath("$[0].number").value("11987654321"));
    }

    @Test
    @DisplayName("GET /api/v1/telephone/organization - should list by organization")
    void shouldFindAllByOrganization() throws Exception {
        when(findAllTelephonesByOrganizationId.execute(any(), anyInt(), anyInt())).thenReturn(List.of(output()));

        mockMvc.perform(get("/api/v1/telephone/organization").param("organizationId", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telephoneId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/telephone/{id} - should find by ID")
    void shouldFindById() throws Exception {
        when(findTelephoneById.execute(id)).thenReturn(output());

        mockMvc.perform(get("/api/v1/telephone/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephoneId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/telephone/{id} - should return 404 when not found")
    void shouldReturn404() throws Exception {
        when(findTelephoneById.execute(id)).thenThrow(new TelephoneNotFoundException(id));

        mockMvc.perform(get("/api/v1/telephone/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/telephone/user - should find by user")
    void shouldFindByUser() throws Exception {
        when(findTelephoneByUserId.execute(userId)).thenReturn(output());

        mockMvc.perform(get("/api/v1/telephone/user").param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/telephone/recyclings - should find by recycling business")
    void shouldFindByRecycling() throws Exception {
        when(findTelephoneByRecyclingBusinessId.execute(recyclingId)).thenReturn(output());

        mockMvc.perform(get("/api/v1/telephone/recyclings").param("recyclingBusinessId", recyclingId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/telephone/user - should register for user")
    void shouldRegisterForUser() throws Exception {
        when(registerUserTelephone.execute(any())).thenReturn(new RegisterTelephoneOutput(id, "11987654321"));

        mockMvc.perform(post("/api/v1/telephone/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"number\":\"11987654321\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telephoneId").value(id.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/telephone/user - should return 400 on blank number")
    void shouldRejectBlankNumber() throws Exception {
        mockMvc.perform(post("/api/v1/telephone/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"number\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/telephone/recyclings - should register for recycling business")
    void shouldRegisterForRecycling() throws Exception {
        when(registerRecyclingTelephone.execute(any())).thenReturn(new RegisterTelephoneOutput(id, "11987654321"));

        mockMvc.perform(post("/api/v1/telephone/recyclings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recyclingBusinessId\":\"" + recyclingId + "\",\"number\":\"11987654321\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PATCH /api/v1/telephone/{id}/number - should change number")
    void shouldChangeNumber() throws Exception {
        mockMvc.perform(patch("/api/v1/telephone/{id}/number", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"1133334444\"}"))
                .andExpect(status().isNoContent());

        verify(changeTelephone).execute(id, "1133334444");
    }

    @Test
    @DisplayName("DELETE /api/v1/telephone/{id} - should delete")
    void shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/telephone/{id}", id))
                .andExpect(status().isNoContent());

        verify(deleteTelephone).execute(id);
    }
}
