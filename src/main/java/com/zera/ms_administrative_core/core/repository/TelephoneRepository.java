package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelephoneRepository {
    Telephone save(Telephone telephone);
    Optional<Telephone> findById(UUID id);
    Optional<Telephone> findByUserId(UUID id);
    Optional<Telephone> findByRecyclingBusinessId(UUID id);

    void delete(Telephone telephone);
    List<Telephone> findAll(int page, int size);
    List<Telephone> findAllByOrganizationId(UUID id, int page, int size);
}
