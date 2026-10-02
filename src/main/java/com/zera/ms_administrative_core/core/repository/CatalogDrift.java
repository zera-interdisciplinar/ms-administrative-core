package com.zera.ms_administrative_core.core.repository;

/** Uma divergencia entre o catalogo de dados e o schema real. */
public record CatalogDrift(String tipo, String tabela, String coluna, String detalhe) {}
