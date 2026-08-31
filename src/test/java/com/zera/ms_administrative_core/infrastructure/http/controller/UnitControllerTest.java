package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.usecase.unit.deleteUnit.DeleteUnit;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindAllUnitsByOrganization;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindUnitById;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.UnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnit;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.renameUnit.RenameUnit;
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

@WebMvcTest(UnitController.class)
class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FindAllUnitsByOrganization findAllUnitsByOrganization;
    @MockitoBean private FindUnitById findUnitById;
    @MockitoBean private RegisterUnit registerUnit;
    @MockitoBean private RenameUnit renameUnit;
    @MockitoBean private DeleteUnit deleteUnit;

    private final UUID id = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    private UnitOutput output() {
        return new UnitOutput(id, "Matriz", organizationId, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/v1/unit - should register a unit")
    void shouldRegister() throws Exception {
        when(registerUnit.execute(any())).thenReturn(new RegisterUnitOutput(id, "Matriz", organizationId));

        mockMvc.perform(post("/api/v1/unit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"" + organizationId + "\",\"name\":\"Matriz\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitId").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Matriz"));
    }

    @Test
    @DisplayName("POST /api/v1/unit - should return 400 on blank name")
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/unit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"" + organizationId + "\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/unit/{id} - should find by ID")
    void shouldFindById() throws Exception {
        when(findUnitById.execute(id)).thenReturn(output());

        mockMvc.perform(get("/api/v1/unit/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/unit/{id} - should return 404 when not found")
    void shouldReturn404() throws Exception {
        when(findUnitById.execute(id)).thenThrow(new UnitNotFoundException(id));

        mockMvc.perform(get("/api/v1/unit/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/unit - should list units by organization")
    void shouldFindAll() throws Exception {
        when(findAllUnitsByOrganization.execute(any(), anyInt(), anyInt())).thenReturn(List.of(output()));

        mockMvc.perform(get("/api/v1/unit").param("organizationId", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitId").value(id.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/unit/{id}/rename - should rename")
    void shouldRename() throws Exception {
        mockMvc.perform(patch("/api/v1/unit/{id}/rename", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Filial\"}"))
                .andExpect(status().isNoContent());

        verify(renameUnit).execute(id, "Filial");
    }

    @Test
    @DisplayName("DELETE /api/v1/unit/{id} - should delete")
    void shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/unit/{id}", id))
                .andExpect(status().isNoContent());

        verify(deleteUnit).execute(id);
    }
}
