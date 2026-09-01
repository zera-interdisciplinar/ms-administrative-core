package com.zera.ms_administrative_core.core.usecase.organization.registerOrganization;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

import java.util.UUID;

public record RegisterOrganizationOutput(
        UUID id,
        String name,
        Cnpj cnpj,
        Email email
) {}
