package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordResetRequest(
        @NotBlank @Email String email
) {
}
