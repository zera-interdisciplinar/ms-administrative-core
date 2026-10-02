package com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;
import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;

@Service
public class RevokeExpiredTokensImpl implements RevokeExpiredTokens {

    private final MaintenanceRepository repository;

    public RevokeExpiredTokensImpl(MaintenanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public TokenCleanup execute(int diasRetencao) {
        if (diasRetencao < 0) {
            throw new IllegalArgumentException("Janela de retencao nao pode ser negativa");
        }
        return repository.revokeExpiredTokens(diasRetencao);
    }
}
