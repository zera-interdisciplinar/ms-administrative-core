package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;

import java.util.List;
import java.util.UUID;

public interface TelephoneRepository {
    Telephone save(Telephone telephone);
    Telephone findById(UUID id);
    Telephone findByUserId(UUID id);
    Telephone findByRecyclingBusinessId(UUID id);

    void delete(Telephone telephone);
    List<Telephone> findAll();
    List<Telephone> findAllByOrganizationId(UUID id);
}
