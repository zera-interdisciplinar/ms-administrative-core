package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UserMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserMapper mapper;

    public UserRepositoryImpl(UserJpaRepository jpa,  UserMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void save(User user) {
        UserJpa entity = mapper.toJpa(user);
        jpa.save(entity);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value())
                .map(mapper::toDomain);
    }

    // UserRepositoryImpl
    @Override
    public List<User> findAll(Role role, Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAllByRoleAndStatus(role, status, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }
}