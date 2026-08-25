package com.zera.ms_administrative_core.core.usecase.organization.registerOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public record RegisterOrganizationCommand(
        String name,
        Cnpj cnpj,
        Email email,
        Plan plan
) {}