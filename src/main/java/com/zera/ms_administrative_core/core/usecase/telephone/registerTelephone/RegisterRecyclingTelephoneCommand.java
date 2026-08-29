package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import java.util.UUID;

public record RegisterRecyclingTelephoneCommand(
        UUID recyclingBusinessId,
        String number
) {}
