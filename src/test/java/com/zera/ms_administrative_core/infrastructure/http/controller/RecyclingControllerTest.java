package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.usecase.recycling.changeRecyclingEmail.ChangeEmail;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindAllRecyclers;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindRecyclingByCnpj;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindRecyclingById;
import com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling.RegisterRecycling;
import com.zera.ms_administrative_core.core.usecase.recycling.renameRecycling.RenameRecycling;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterRecyclingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecyclingController.class)
class RecyclingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private RegisterRecycling registerRecycling;
    @MockitoBean private FindAllRecyclers findAllRecyclers;
    @MockitoBean private FindRecyclingById findRecyclingById;
    @MockitoBean private FindRecyclingByCnpj findRecyclingByCnpj;
    @MockitoBean private RenameRecycling renameRecycling;
    @MockitoBean private ChangeEmail changeEmail;

    private RecyclingBusiness business;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        business = new RecyclingBusiness(id, "Test", new Cnpj("11.222.333/0001-81"), new Email("test@test.com"));
    }

    @Test
    @DisplayName("POST /api/v1/recyclings - should register recycling business")
    void shouldRegister() throws Exception {
        RegisterRecyclingRequest request = new RegisterRecyclingRequest("Test", "11.222.333/0001-81", "test@test.com");
        when(registerRecycling.execute(anyString(), anyString(), anyString())).thenReturn(business);

        mockMvc.perform(post("/api/v1/recyclings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    @DisplayName("GET /api/v1/recyclings - should find all")
    void shouldFindAll() throws Exception {
        when(findAllRecyclers.execute()).thenReturn(List.of(business));

        mockMvc.perform(get("/api/v1/recyclings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/recyclings/{id} - should find by ID")
    void shouldFindById() throws Exception {
        when(findRecyclingById.execute(id)).thenReturn(business);

        mockMvc.perform(get("/api/v1/recyclings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/recyclings/cnpj/{cnpj} - should find by CNPJ")
    void shouldFindByCnpj() throws Exception {
        String cnpj = "11222333000181";
        when(findRecyclingByCnpj.execute(cnpj)).thenReturn(business);

        mockMvc.perform(get("/api/v1/recyclings/cnpj/{cnpj}", cnpj))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    @DisplayName("PATCH /api/v1/recyclings/{id}/name - should rename")
    void shouldRename() throws Exception {
        mockMvc.perform(patch("/api/v1/recyclings/{id}/name", id)
                .param("name", "New Name"))
                .andExpect(status().isNoContent());

        verify(renameRecycling).execute(id, "New Name");
    }

    @Test
    @DisplayName("PATCH /api/v1/recyclings/{id}/email - should change email")
    void shouldChangeEmail() throws Exception {
        mockMvc.perform(patch("/api/v1/recyclings/{id}/email", id)
                .param("email", "new@test.com"))
                .andExpect(status().isNoContent());

        verify(changeEmail).execute(id, "new@test.com");
    }
}
