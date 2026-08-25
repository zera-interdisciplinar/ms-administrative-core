package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.organization.activateOrganization.ActivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail.ChangeOrganizationEmail;
import com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization.DeactivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.*;
import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganizationOutput;
import com.zera.ms_administrative_core.core.usecase.organization.renameOrganization.RenameOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.suspendOrganization.SuspendOrganization;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {
    private final FindAllOrganizations findAllOrganizations;
    private final RegisterOrganization registerOrganization;
    private final FindOrganizationById findOrganizationById;
    private final FindOrganizationByCnpj findOrganizationByCnpj;
    private final FindOrganizationByEmail findOrganizationByEmail;
    private final RenameOrganization renameOrganization;
    private final ChangeOrganizationEmail changeOrganizationEmail;
    private final ActivateOrganization activateOrganization;
    private final DeactivateOrganization deactivateUser;
    private final SuspendOrganization suspendOrganization;

    public OrganizationController(
            FindAllOrganizations findAllOrganizations,
            RegisterOrganization registerOrganization,
            FindOrganizationById findOrganizationById,
            FindOrganizationByCnpj findOrganizationByCnpj,
            FindOrganizationByEmail findOrganizationByEmail,
            RenameOrganization renameOrganization,
            ChangeOrganizationEmail changeOrganizationEmail,
            ActivateOrganization activateOrganization,
            DeactivateOrganization deactivateUser,
            SuspendOrganization suspendOrganization) {

        this.findAllOrganizations = findAllOrganizations;
        this.registerOrganization = registerOrganization;
        this.findOrganizationById = findOrganizationById;
        this.findOrganizationByCnpj = findOrganizationByCnpj;
        this.findOrganizationByEmail = findOrganizationByEmail;
        this.renameOrganization = renameOrganization;
        this.changeOrganizationEmail = changeOrganizationEmail;
        this.activateOrganization = activateOrganization;
        this.deactivateUser = deactivateUser;
        this.suspendOrganization = suspendOrganization;
    }
    
    @GetMapping
    public ResponseEntity<List<OrganizationOutput>> findAll(
            @RequestParam(required = false) Plan plan,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cnpj,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ) {
        if (cnpj != null && email == null) {
            return ResponseEntity.ok(List.of(findOrganizationByCnpj.execute(cnpj)));
        }
        else if (email != null && cnpj == null) {
            return ResponseEntity.ok(List.of(findOrganizationByEmail.execute(email)));
        }
        return ResponseEntity.ok(findAllOrganizations.execute(plan, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationOutput> findById( @PathVariable UUID id ){
        OrganizationOutput output = findOrganizationById.execute(id);
        return ResponseEntity.ok(output);
    }

    @PostMapping
    public ResponseEntity<RegisterOrganizationOutput> register(
            @RequestBody @Valid RegisterOrganizationRequest request
    ) {
        RegisterOrganizationOutput output = registerOrganization.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(output.id())
                .toUri();
        return ResponseEntity.created(location).body(output);
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable UUID id,
            @RequestParam String newName
    ) {
        renameOrganization.execute(id, newName);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<Void> changeEmail(
            @PathVariable UUID id,
            @RequestParam String newEmail
    ) {
        changeOrganizationEmail.execute(id, newEmail);
        return ResponseEntity.noContent().build();
    }


}
