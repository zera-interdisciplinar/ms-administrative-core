package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.InvitationMapper;

@Repository
public class InvitationRepositoryImpl implements InvitationRepository {

    private final InvitationJpaRepository jpa;
    private final InvitationMapper mapper;

    public InvitationRepositoryImpl(InvitationJpaRepository jpa, InvitationMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Invitation save(Invitation invitation) {
        return mapper.toDomain(jpa.save(mapper.toJpa(invitation)));
    }

    @Override
    public Optional<Invitation> findPendingByCode(String code) {
        return jpa.findByCodeAndStatus(code, InvitationStatus.PENDING).map(mapper::toDomain);
    }

    @Override
    public List<Invitation> findAllPendingByManager(UUID managerId, LocalDateTime now) {
        return jpa.findAllByManagerIdAndStatusAndExpiresAtAfterOrderByExpiresAtAsc(managerId, InvitationStatus.PENDING, now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
