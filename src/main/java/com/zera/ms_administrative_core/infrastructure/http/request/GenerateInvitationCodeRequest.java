package com.zera.ms_administrative_core.infrastructure.http.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record GenerateInvitationCodeRequest(
        @NotNull UUID managerId
) {}
