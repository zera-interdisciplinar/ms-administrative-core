package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.TelephoneJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.TelephoneMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TelephoneRepositoryImpl implements TelephoneRepository {

    private final TelephoneJpaRepository jpa;
    private final TelephoneMapper mapper;

    public TelephoneRepositoryImpl(TelephoneJpaRepository jpa, TelephoneMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Telephone save(Telephone telephone) {
        TelephoneJpa entity = mapper.toJpa(telephone);
        TelephoneJpa saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Telephone> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Telephone> findByUserId(UUID id) {
        return jpa.findByUserId(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Telephone> findByRecyclingBusinessId(UUID id) {
        return jpa.findByRecyclingBusinessId(id)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(Telephone telephone) {
        jpa.delete(mapper.toJpa(telephone));
    }

    @Override
    public List<Telephone> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAll(pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Telephone> findAllByOrganizationId(UUID id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAllByOrganizationId(id, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
