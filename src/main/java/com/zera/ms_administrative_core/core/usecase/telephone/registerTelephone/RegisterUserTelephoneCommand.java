package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import java.util.UUID;

public record RegisterUserTelephoneCommand(
        UUID userId,
        String number
) {}