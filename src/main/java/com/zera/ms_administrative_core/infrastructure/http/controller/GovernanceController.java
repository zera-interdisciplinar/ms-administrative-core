package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.repository.CatalogColumn;
import com.zera.ms_administrative_core.core.repository.CatalogDrift;
import com.zera.ms_administrative_core.core.repository.CatalogTable;
import com.zera.ms_administrative_core.core.usecase.governance.findDataCatalog.FindDataCatalog;
import com.zera.ms_administrative_core.infrastructure.security.Authz;

/**
 * Catalogo de dados exposto por HTTP.
 *
 * <p>O endpoint de divergencia e o que torna o catalogo verificavel fora do teste: se ele devolver
 * lista nao vazia em producao, o schema andou e o catalogo ficou atras.
 */
@RestController
@RequestMapping("/api/v1/governance/data-catalog")
public class GovernanceController {

    private final FindDataCatalog findDataCatalog;

    public GovernanceController(FindDataCatalog findDataCatalog) {
        this.findDataCatalog = findDataCatalog;
    }

    @GetMapping
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<CatalogTable>> tables() {
        return ResponseEntity.ok(findDataCatalog.tables());
    }

    @GetMapping("/{tabela}/columns")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<CatalogColumn>> columns(@PathVariable String tabela) {
        return ResponseEntity.ok(findDataCatalog.columns(tabela));
    }

    @GetMapping("/drift")
    @PreAuthorize(Authz.MANAGER)
    public ResponseEntity<List<CatalogDrift>> drift() {
        return ResponseEntity.ok(findDataCatalog.drift());
    }
}
