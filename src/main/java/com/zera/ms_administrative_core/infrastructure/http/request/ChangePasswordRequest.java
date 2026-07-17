package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String rawPassword,
        @NotBlank String confirmPassword
) {}