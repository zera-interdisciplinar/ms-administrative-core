package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterRecyclingTelephoneCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterRecyclingTelephoneRequest(
        @NotNull UUID recyclingBusinessId,
        @NotBlank String number
        ) {
    public RegisterRecyclingTelephoneCommand toCommand() {
        return new RegisterRecyclingTelephoneCommand(recyclingBusinessId, number);
    }
}
