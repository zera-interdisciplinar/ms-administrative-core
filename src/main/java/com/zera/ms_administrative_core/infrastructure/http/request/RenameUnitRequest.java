package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;

public record RenameUnitRequest(
        @NotBlank String name
) {
}
