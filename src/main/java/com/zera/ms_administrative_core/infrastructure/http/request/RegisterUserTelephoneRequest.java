package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone.RegisterUserTelephoneCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUserTelephoneRequest(
        @NotNull UUID userId,
        @NotBlank String number
) {
    public RegisterUserTelephoneCommand toCommand(){
        return new RegisterUserTelephoneCommand(userId, number);
    }
}
