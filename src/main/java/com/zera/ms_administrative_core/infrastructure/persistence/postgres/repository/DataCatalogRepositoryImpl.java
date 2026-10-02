package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.repository.DataCatalogRepository;

@Repository
public class DataCatalogRepositoryImpl implements DataCatalogRepository {

    private static final RowMapper<CatalogTable> TABELA = (rs, i) -> new CatalogTable(
            rs.getString("tabela"), rs.getString("dominio"), rs.getString("descricao"),
            rs.getString("regra_negocio"), rs.getString("nivel_acesso"), rs.getString("origem"));

    private static final RowMapper<CatalogColumn> COLUNA = (rs, i) -> new CatalogColumn(
            rs.getString("tabela"), rs.getString("coluna"), rs.getString("descricao"),
            rs.getString("regra_negocio"), rs.getString("nivel_acesso"), rs.getBoolean("contem_pii"));

    private static final RowMapper<CatalogDrift> DIVERGENCIA = (rs, i) -> new CatalogDrift(
            rs.getString("tipo"), rs.getString("tabela"), rs.getString("coluna"), rs.getString("detalhe"));

    private final JdbcTemplate jdbc;

    public DataCatalogRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CatalogTable> findTables() {
        return jdbc.query("""
                SELECT tabela, dominio, descricao, regra_negocio, nivel_acesso, origem
                  FROM catalogo_tabela ORDER BY dominio, tabela
                """, TABELA);
    }

    @Override
    public List<CatalogColumn> findColumns(String tabela) {
        return jdbc.query("""
                SELECT tabela, coluna, descricao, regra_negocio, nivel_acesso, contem_pii
                  FROM catalogo_coluna WHERE tabela = ? ORDER BY coluna
                """, COLUNA, tabela);
    }

    @Override
    public List<CatalogDrift> findDrift() {
        return jdbc.query("SELECT tipo, tabela, coluna, detalhe FROM fn_catalogo_divergencia()",
                DIVERGENCIA);
    }
}
