package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.usecase.telephone.changeTelephone.ChangeTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.deleteTelephone.DeleteTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.*;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterRecyclingTelephone;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterTelephoneOutput;
import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterUserTelephone;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterRecyclingTelephoneRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterUserTelephoneRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telephone")
public class TelephoneController {
    private final ChangeTelephone changeTelephone;
    private final DeleteTelephone deleteTelephone;
    private final FindAllTelephones findAllTelephones;
    private final FindAllTelephonesByOrganizationId findAllTelephonesByOrganizationId;
    private final FindTelephoneById findTelephoneById;
    private final FindTelephoneByRecyclingBusinessId findTelephoneByRecyclingBusinessId;
    private final FindTelephoneByUserId findTelephoneByUserId;
    private final RegisterRecyclingTelephone registerRecyclingTelephone;
    private final RegisterUserTelephone registerUserTelephone;

    public TelephoneController(ChangeTelephone changeTelephone,
                               DeleteTelephone deleteTelephone,
                               FindAllTelephones findAllTelephones,
                               FindAllTelephonesByOrganizationId findAllTelephonesByOrganizationId,
                               FindTelephoneById findTelephoneById,
                               FindTelephoneByRecyclingBusinessId findTelephoneByRecyclingBusinessId,
                               FindTelephoneByUserId findTelephoneByUserId,
                               RegisterRecyclingTelephone registerRecyclingTelephone,
                               RegisterUserTelephone registerUserTelephone) {
        this.changeTelephone = changeTelephone;
        this.deleteTelephone = deleteTelephone;
        this.findAllTelephones = findAllTelephones;
        this.findAllTelephonesByOrganizationId = findAllTelephonesByOrganizationId;
        this.findTelephoneById = findTelephoneById;
        this.findTelephoneByRecyclingBusinessId = findTelephoneByRecyclingBusinessId;
        this.findTelephoneByUserId = findTelephoneByUserId;
        this.registerRecyclingTelephone = registerRecyclingTelephone;
        this.registerUserTelephone = registerUserTelephone;
    }

    @GetMapping
    public ResponseEntity<List<TelephoneOutput>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(findAllTelephones.execute(page, size));
    }

    @GetMapping("/organization")
    public ResponseEntity<List<TelephoneOutput>> findAllByOrganization(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(findAllTelephonesByOrganizationId.execute(organizationId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelephoneOutput> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findTelephoneById.execute(id));
    }

    @PostMapping("/user")
    public ResponseEntity<RegisterTelephoneOutput> registerForUser(
            @RequestBody @Valid RegisterUserTelephoneRequest request
            ){
        RegisterTelephoneOutput output = registerUserTelephone.execute(request.toCommand());
        return ResponseEntity.ok(output);
    }

    @PostMapping("/recyclings")
    public ResponseEntity<RegisterTelephoneOutput> registerForRecycling(
        @RequestBody @Valid RegisterRecyclingTelephoneRequest request
    ){
        RegisterTelephoneOutput output = registerRecyclingTelephone.execute(request.toCommand());
        return ResponseEntity.ok(output);
    }


}
