package com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog;

import java.util.List;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;

public interface FindDataCatalog {

    List<CatalogTable> tables();

    List<CatalogColumn> columns(String tabela);

    List<CatalogDrift> drift();
}
