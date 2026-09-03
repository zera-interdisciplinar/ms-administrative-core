package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganizationCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterOrganizationRequest(
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String email,
        @NotNull Plan plan
) {
    public RegisterOrganizationCommand toCommand() {
        return new RegisterOrganizationCommand(name, new Cnpj(cnpj), new Email(email), plan);
    }
}
