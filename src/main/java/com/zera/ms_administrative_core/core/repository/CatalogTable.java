package com.zera.ms_administrative_core.core.repository;

/** Uma linha de {@code catalogo_tabela}. */
public record CatalogTable(
        String tabela,
        String dominio,
        String descricao,
        String regraNegocio,
        String nivelAcesso,
        String origem) {}
