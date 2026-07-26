package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.usecase.recycling.changeRecyclingEmail.ChangeEmail;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindAllRecyclers;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindRecyclingByCnpj;
import com.zera.ms_administrative_core.core.usecase.recycling.findRecycling.FindRecyclingById;
import com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling.RegisterRecycling;
import com.zera.ms_administrative_core.core.usecase.recycling.renameRecycling.RenameRecycling;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterRecyclingRequest;

@RestController
@RequestMapping("/api/v1/recyclings")
public class RecyclingController {

    private final RegisterRecycling registerRecycling;
    private final FindAllRecyclers findAllRecyclers;
    private final FindRecyclingById findRecyclingById;
    private final FindRecyclingByCnpj findRecyclingByCnpj;
    private final RenameRecycling renameRecycling;
    private final ChangeEmail changeEmail;

    public RecyclingController(
            RegisterRecycling registerRecycling,
            FindAllRecyclers findAllRecyclers,
            FindRecyclingById findRecyclingById,
            FindRecyclingByCnpj findRecyclingByCnpj,
            RenameRecycling renameRecycling,
            ChangeEmail changeEmail) {

        this.registerRecycling = registerRecycling;
        this.findAllRecyclers = findAllRecyclers;
        this.findRecyclingById = findRecyclingById;
        this.findRecyclingByCnpj = findRecyclingByCnpj;
        this.renameRecycling = renameRecycling;
        this.changeEmail = changeEmail;
    }

    @PostMapping
    public ResponseEntity<RecyclingBusiness> register(
            @RequestBody RegisterRecyclingRequest request) {

        RecyclingBusiness created = registerRecycling.execute(
                request.name(),
                request.cnpj(),
                request.email());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RecyclingBusiness>> findAll() {
        return ResponseEntity.ok(findAllRecyclers.execute());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecyclingBusiness> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findRecyclingById.execute(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<RecyclingBusiness> findByCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(findRecyclingByCnpj.execute(cnpj));
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<Void> rename(
            @PathVariable UUID id,
            @RequestParam String name) {

        renameRecycling.execute(id, name);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<Void> changeEmail(
            @PathVariable UUID id,
            @RequestParam String email) {

        changeEmail.execute(id, email);
        return ResponseEntity.noContent().build();
    }
}