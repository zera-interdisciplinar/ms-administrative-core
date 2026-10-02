package com.zera.ms_administrative_core.core.repository;

import java.util.List;

/** Porta para o catalogo de dados e para o confronto dele com o schema real. */
public interface DataCatalogRepository {

    List<CatalogTable> findTables();

    List<CatalogColumn> findColumns(String tabela);

    /** Divergencias entre catalogo e {@code information_schema}. Lista vazia = catalogo em dia. */
    List<CatalogDrift> findDrift();
}
