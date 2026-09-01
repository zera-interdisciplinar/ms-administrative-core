package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import jakarta.validation.constraints.NotNull;

public record ChangeOrganizationPlanRequest(
        @NotNull Plan plan
) {
}
