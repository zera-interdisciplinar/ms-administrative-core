package com.zera.ms_administrative_core.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;

public interface RecyclingBusinessRepository {
    RecyclingBusiness save(RecyclingBusiness recyclingBusiness);
    Optional<RecyclingBusiness> findById(UUID id);
    Optional<RecyclingBusiness> findByCnpj(Cnpj cnpj);
    List<RecyclingBusiness> findAll();
    void delete(UUID id);
}
