package com.zera.ms_administrative_core.infrastructure.http.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog.FindDataCatalog;

@WebMvcTest(GovernanceController.class)
class GovernanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindDataCatalog findDataCatalog;

    @Test
    @DisplayName("GET /governance/data-catalog - deve listar tabelas com nivel de acesso")
    void shouldListTables() throws Exception {
        when(findDataCatalog.tables()).thenReturn(List.of(new CatalogTable(
                "refresh_token", "Identidade", "Refresh token opaco",
                "Guarda apenas o SHA-256", "SECRETO", "APLICACAO")));

        mockMvc.perform(get("/api/v1/governance/data-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tabela").value("refresh_token"))
                .andExpect(jsonPath("$[0].nivelAcesso").value("SECRETO"));
    }

    @Test
    @DisplayName("GET /governance/data-catalog/{tabela}/columns - deve marcar PII")
    void shouldListColumns() throws Exception {
        when(findDataCatalog.columns("user_account")).thenReturn(List.of(new CatalogColumn(
                "user_account", "email", "E-mail de login", "Identificador de autenticacao",
                "CONFIDENCIAL", true)));

        mockMvc.perform(get("/api/v1/governance/data-catalog/{tabela}/columns", "user_account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contemPii").value(true));
    }

    @Test
    @DisplayName("GET /governance/data-catalog/drift - catalogo em dia deve devolver lista vazia")
    void shouldReturnEmptyDrift() throws Exception {
        when(findDataCatalog.drift()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/governance/data-catalog/drift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /governance/data-catalog/drift - deve expor a divergencia encontrada")
    void shouldReportDrift() throws Exception {
        when(findDataCatalog.drift()).thenReturn(List.of(new CatalogDrift(
                "TABELA_NAO_CATALOGADA", "nova_tabela", null, "Existe no banco")));

        mockMvc.perform(get("/api/v1/governance/data-catalog/drift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TABELA_NAO_CATALOGADA"));
    }
}
