package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String rawPassword,
        @NotNull Role role,
        @NotNull UUID unitId
) {
    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(name, role, rawPassword, email, unitId);
    }
}