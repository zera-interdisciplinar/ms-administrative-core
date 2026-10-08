package com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;

public interface RevokeExpiredTokens {
    TokenCleanup execute(int diasRetencao);
}
