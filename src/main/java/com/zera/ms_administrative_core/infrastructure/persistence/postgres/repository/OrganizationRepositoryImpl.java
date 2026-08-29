package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.OrganizationJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.OrganizationMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {

    private final OrganizationJpaRepository jpa;
    private final OrganizationMapper mapper;

    public OrganizationRepositoryImpl(OrganizationJpaRepository jpa, OrganizationMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Organization save(Organization organization) {
        OrganizationJpa entity = mapper.toJpa(organization);
        OrganizationJpa saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Organization> findByCnpj(Cnpj cnpj) {
        return jpa.findByCnpj(cnpj.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Organization> findByEmail(Email email) {
        return jpa.findByEmail(email.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpa.existsByCnpj(cnpj.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public List<Organization> findAll(Plan plan, Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAllByPlanAndStatus(plan, status, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
