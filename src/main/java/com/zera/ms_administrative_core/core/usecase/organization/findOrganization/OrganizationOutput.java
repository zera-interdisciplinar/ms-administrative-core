package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationOutput(
        UUID organizationId,
        String name,
        Cnpj cnpj,
        Email email,
        Plan plan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrganizationOutput from(Organization org) {
        return new OrganizationOutput(
                org.getOrganizationId(),
                org.getName(),
                org.getCnpj(),
                org.getEmail(),
                org.getPlan(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
