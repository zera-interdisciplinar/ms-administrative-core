package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.repository.DataCatalogRepository;

/**
 * Prova o catalogo da V17.
 *
 * <p>O teste que importa aqui e o de DIVERGENCIA: e ele que transforma "temos documentacao" em algo
 * que quebra o build quando alguem adiciona coluna sem documentar. Sem ele, o catalogo viraria
 * ficcao na primeira migracao seguinte.
 */
class DataCatalogIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataCatalogRepository catalogo;

    @Test
    @DisplayName("Catalogo deve estar em dia com o schema real (nenhuma divergencia)")
    void catalogShouldMatchSchema() {
        List<CatalogDrift> divergencias = catalogo.findDrift();

        assertThat(divergencias)
                .as("Coluna ou tabela nova sem linha no catalogo. Atualize a V17 (ou crie uma nova "
                        + "migracao de catalogo) no mesmo commit que altera o schema.")
                .isEmpty();
    }

    @Test
    @DisplayName("Deve catalogar todas as tabelas de dominio, com nivel de acesso valido")
    void shouldCatalogAllTables() {
        List<CatalogTable> tabelas = catalogo.findTables();

        assertThat(tabelas).isNotEmpty();
        assertThat(tabelas).extracting(CatalogTable::tabela)
                .contains("organization", "unit", "user_account", "alert", "refresh_token",
                        "audit_log", "user_access_log", "usuario_ativo_diario");
        assertThat(tabelas).allSatisfy(t -> assertThat(t.nivelAcesso())
                .isIn("INTERNO", "RESTRITO", "CONFIDENCIAL", "SECRETO"));
        assertThat(tabelas).allSatisfy(t -> assertThat(t.descricao()).isNotBlank());
    }

    /** Segredo classificado como "interno" e pior que ausencia de classificacao: passa confianca falsa. */
    @Test
    @DisplayName("Hash de senha e de refresh token devem estar classificados como SECRETO")
    void shouldClassifySecretsAsSecret() {
        CatalogColumn senha = coluna("user_account", "password");
        CatalogColumn token = coluna("refresh_token", "token_hash");

        assertThat(senha.nivelAcesso()).isEqualTo("SECRETO");
        assertThat(senha.contemPii()).isTrue();
        assertThat(token.nivelAcesso()).isEqualTo("SECRETO");
    }

    @Test
    @DisplayName("Colunas com dado pessoal devem estar marcadas como PII")
    void shouldFlagPii() {
        assertThat(coluna("user_account", "email").contemPii()).isTrue();
        assertThat(coluna("user_account", "name").contemPii()).isTrue();
        assertThat(coluna("organization", "cnpj").contemPii()).isTrue();
        // contraprova: id nao e dado pessoal, e marcar tudo como PII tornaria a marca inutil
        assertThat(coluna("user_account", "id").contemPii()).isFalse();
    }

    @Test
    @DisplayName("Tabela legada deve estar documentada como legado")
    void shouldDocumentLegacyTable() {
        CatalogTable legado = catalogo.findTables().stream()
                .filter(t -> t.tabela().equals("disposal_report")).findFirst().orElseThrow();

        assertThat(legado.origem()).isEqualTo("LEGADO");
        assertThat(legado.regraNegocio()).contains("ms-inventory");
    }

    private CatalogColumn coluna(String tabela, String nome) {
        return catalogo.findColumns(tabela).stream()
                .filter(c -> c.coluna().equals(nome)).findFirst()
                .orElseThrow(() -> new AssertionError("coluna nao catalogada: " + tabela + "." + nome));
    }
}
