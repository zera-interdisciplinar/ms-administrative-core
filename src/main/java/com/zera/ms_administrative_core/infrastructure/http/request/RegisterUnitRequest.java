package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUnitRequest(
        @NotNull UUID organizationId,
        @NotBlank String name
        ) {
    public RegisterUnitCommand toCommand() {
        return new RegisterUnitCommand(organizationId, name);
    }
}
