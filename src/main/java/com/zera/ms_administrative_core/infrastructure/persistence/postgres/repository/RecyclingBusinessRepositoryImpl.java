package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RecyclingBusinessJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.RecyclingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("recyclingBusinessRepositoryImpl")
public class RecyclingBusinessRepositoryImpl implements RecyclingBusinessRepository {

    private final RecyclingJpaRepository jpa;
    private final RecyclingMapper mapper;

    public RecyclingBusinessRepositoryImpl(RecyclingJpaRepository jpa, RecyclingMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public RecyclingBusiness save(RecyclingBusiness recyclingBusiness) {
        RecyclingBusinessJpa entity = mapper.toJpa(recyclingBusiness);
        RecyclingBusinessJpa saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RecyclingBusiness> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RecyclingBusiness> findByCnpj(Cnpj cnpj) {
        return jpa.findByCnpj(cnpj.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<RecyclingBusiness> findAll() {
        return jpa.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
