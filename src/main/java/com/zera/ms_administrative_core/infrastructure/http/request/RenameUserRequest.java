package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;

public record RenameUserRequest(
        @NotBlank String name
) {}
