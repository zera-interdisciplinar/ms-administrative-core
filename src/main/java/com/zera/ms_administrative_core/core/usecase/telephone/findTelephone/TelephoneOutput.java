package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;

import java.time.LocalDateTime;
import java.util.UUID;

public record TelephoneOutput(
        UUID telephoneId,
        TelephoneNumber number,
        UUID userId,
        UUID organizationId,
        UUID unitId,
        UUID recyclingBusinessId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TelephoneOutput from(Telephone telephone) {
        return new TelephoneOutput(
                telephone.getTelephoneId(),
                telephone.getNumber(),
                telephone.getUserId(),
                telephone.getOrganizationId(),
                telephone.getUnitId(),
                telephone.getRecyclingBusinessId(),
                telephone.getCreatedAt(),
                telephone.getUpdatedAt()
        );
    }
}
