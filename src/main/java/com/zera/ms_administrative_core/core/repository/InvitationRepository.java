package com.zera.ms_administrative_core.core.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findPendingByCode(String code);
    List<Invitation> findAllPendingByManager(UUID managerId, LocalDateTime now);
}
