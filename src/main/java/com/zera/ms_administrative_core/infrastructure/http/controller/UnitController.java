package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.usecase.unit.deleteUnit.DeleteUnit;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindAllUnitsByOrganization;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindUnitById;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.UnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnit;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.renameUnit.RenameUnit;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterUnitRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RenameUnitRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unit")
public class UnitController {
    private final FindAllUnitsByOrganization findAllUnitsByOrganization;
    private final FindUnitById findUnitById;
    private final RegisterUnit registerUnit;
    private final RenameUnit renameUnit;
    private final DeleteUnit deleteUnit;


    public UnitController(
            FindAllUnitsByOrganization findAllUnitsByOrganization,
            FindUnitById findUnitById,
            RegisterUnit registerUnit,
            RenameUnit renameUnit,
            DeleteUnit deleteUnit) {

        this.findAllUnitsByOrganization = findAllUnitsByOrganization;
        this.findUnitById = findUnitById;
        this.registerUnit = registerUnit;
        this.renameUnit = renameUnit;
        this.deleteUnit = deleteUnit;
    }

    @PostMapping
    public ResponseEntity<RegisterUnitOutput> register(
            @RequestBody @Valid RegisterUnitRequest request
        ){
        RegisterUnitOutput output = registerUnit.execute(request.toCommand());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(output.unitId())
                .toUri();

        return ResponseEntity.created(location).body(output);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitOutput> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findUnitById.execute(id));
    }

    @GetMapping
    public ResponseEntity<List<UnitOutput>> findAll(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ) {

        return ResponseEntity.ok(findAllUnitsByOrganization.execute(organizationId, page, size));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable UUID id,
            @RequestBody @Valid RenameUnitRequest request
    ) {
        renameUnit.execute(id, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUnit.execute(id);
        return ResponseEntity.noContent().build();
    }

}
