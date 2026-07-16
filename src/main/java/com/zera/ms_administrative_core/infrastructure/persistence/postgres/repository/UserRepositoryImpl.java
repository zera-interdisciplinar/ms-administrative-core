package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UserMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return jpa.findByEmail(email.toString())
                .map(mapper::toDomain);
    }

    @Override
    public List<User> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAll(pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.toString());
    }
}