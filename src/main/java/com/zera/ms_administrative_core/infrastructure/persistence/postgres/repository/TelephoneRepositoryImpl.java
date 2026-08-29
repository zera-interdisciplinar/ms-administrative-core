package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TelephoneRepositoryImpl implements TelephoneRepository {



    @Override
    public Telephone save(Telephone telephone) {
        return null;
    }

    @Override
    public Optional<Telephone> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Telephone> findByUserId(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Telephone> findByRecyclingBusinessId(UUID id) {
        return Optional.empty();
    }

    @Override
    public void delete(Telephone telephone) {

    }

    @Override
    public List<Telephone> findAll() {
        return List.of();
    }

    @Override
    public List<Telephone> findAllByOrganizationId(UUID id) {
        return List.of();
    }
}
