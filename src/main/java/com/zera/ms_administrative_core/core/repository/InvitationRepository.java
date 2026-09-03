package com.zera.ms_administrative_core.core.repository;

import java.util.Optional;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findPendingByCode(String code);
}
