package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;

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
}
