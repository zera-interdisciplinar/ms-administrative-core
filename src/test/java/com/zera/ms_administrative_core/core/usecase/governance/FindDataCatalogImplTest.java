package com.zera.ms_administrative_core.core.usecase.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.repository.DataCatalogRepository;
import com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog.FindDataCatalogImpl;

@ExtendWith(MockitoExtension.class)
class FindDataCatalogImplTest {

    @Mock
    private DataCatalogRepository repository;

    @InjectMocks
    private FindDataCatalogImpl useCase;

    @Test
    @DisplayName("Deve listar as tabelas catalogadas")
    void shouldListTables() {
        when(repository.findTables()).thenReturn(List.of(new CatalogTable(
                "alert", "Alertas", "Alerta gerado pelo inventory", "Dedup por rule/event",
                "RESTRITO", "APLICACAO")));

        assertThat(useCase.tables()).singleElement()
                .extracting(CatalogTable::nivelAcesso).isEqualTo("RESTRITO");
    }

    @Test
    @DisplayName("Deve listar as colunas de uma tabela")
    void shouldListColumns() {
        when(repository.findColumns("user_account")).thenReturn(List.of(new CatalogColumn(
                "user_account", "password", "Hash bcrypt", "Nunca sai do servico", "SECRETO", true)));

        assertThat(useCase.columns("user_account")).singleElement()
                .extracting(CatalogColumn::contemPii).isEqualTo(true);
    }

    @Test
    @DisplayName("Deve recusar nome de tabela vazio sem consultar o banco")
    void shouldRejectBlankTable() {
        assertThatThrownBy(() -> useCase.columns("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.columns(null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    /** Lista vazia e o estado saudavel: catalogo e schema batem. */
    @Test
    @DisplayName("Deve devolver lista vazia quando o catalogo esta em dia")
    void shouldReturnEmptyDriftWhenHealthy() {
        when(repository.findDrift()).thenReturn(List.of());

        assertThat(useCase.drift()).isEmpty();
    }

    @Test
    @DisplayName("Deve reportar divergencia quando o schema anda e o catalogo fica atras")
    void shouldReportDrift() {
        when(repository.findDrift()).thenReturn(List.of(new CatalogDrift(
                "COLUNA_NAO_CATALOGADA", "alert", "nova_coluna", "Existe no banco e nao esta no catalogo")));

        assertThat(useCase.drift()).singleElement()
                .extracting(CatalogDrift::tipo).isEqualTo("COLUNA_NAO_CATALOGADA");
    }
}
