package com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.repository.DataCatalogRepository;

@Service
public class FindDataCatalogImpl implements FindDataCatalog {

    private final DataCatalogRepository repository;

    public FindDataCatalogImpl(DataCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CatalogTable> tables() {
        return repository.findTables();
    }

    @Override
    public List<CatalogColumn> columns(String tabela) {
        if (tabela == null || tabela.isBlank()) {
            throw new IllegalArgumentException("Nome de tabela obrigatorio");
        }
        return repository.findColumns(tabela);
    }

    @Override
    public List<CatalogDrift> drift() {
        return repository.findDrift();
    }
}
