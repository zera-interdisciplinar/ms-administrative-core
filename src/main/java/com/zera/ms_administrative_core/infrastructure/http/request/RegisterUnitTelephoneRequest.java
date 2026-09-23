package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterUnitTelephoneCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUnitTelephoneRequest(
        @NotNull UUID unitId,
        @NotBlank String number
) {
    public RegisterUnitTelephoneCommand toCommand() {
        return new RegisterUnitTelephoneCommand(unitId, number);
    }
}
