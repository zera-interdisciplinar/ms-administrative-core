package com.zera.ms_administrative_core.core.repository;

/** Uma linha de {@code catalogo_coluna}. */
public record CatalogColumn(
        String tabela,
        String coluna,
        String descricao,
        String regraNegocio,
        String nivelAcesso,
        boolean contemPii) {}
