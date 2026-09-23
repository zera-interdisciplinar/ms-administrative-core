package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterOrganizationTelephoneCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterOrganizationTelephoneRequest(
        @NotNull UUID organizationId,
        @NotBlank String number
) {
    public RegisterOrganizationTelephoneCommand toCommand() {
        return new RegisterOrganizationTelephoneCommand(organizationId, number);
    }
}
