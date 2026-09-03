package com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenerateInvitationCodeOutput(
    UUID id,
    String code,
    UUID managerId,
    UUID unitId,
    LocalDateTime expiresAt
) {}
