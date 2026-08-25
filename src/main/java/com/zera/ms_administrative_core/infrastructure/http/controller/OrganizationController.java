package com.zera.ms_administrative_core.infrastructure.http.controller;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.usecase.organization.activateOrganization.ActivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail.ChangeOrganizationEmail;
import com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization.DeactivateOrganization;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindAllOrganizations;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationByCnpj;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationByEmail;
import com.zera.ms_administrative_core.core.usecase.organization.findOrganization.FindOrganizationById;
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
    public Organization getAllOrganizations() {
        return null;
    }

    @PostMapping
    public ResponseEntity<RegisterOrganizationOutput> createOrganization(
            @RequestBody @Valid RegisterOrganizationRequest request) {
        RegisterOrganizationOutput output = registerOrganization.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(output.id())
                .toUri();
        return ResponseEntity.created(location).body(output);
    }
}
