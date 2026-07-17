package com.zera.ms_administrative_core.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.UserRepository;

public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> users = new LinkedHashMap<>();

    @Override
    public void save(User user) {
        users.put(user.getUserId(), user);
    }

    @Override
    public void delete(UUID id) {
        users.remove(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findAll(Role role, Status status, int page, int size) {
        List<User> filtered = users.values().stream()
                .filter(user -> role == null || user.role().equals(role))
                .filter(user -> status == null || user.getStatus().equals(status))
                .toList();

        int start = Math.max(page, 0) * Math.max(size, 1);
        int end = Math.min(start + Math.max(size, 1), filtered.size());

        if (start >= end) {
            return List.of();
        }

        return new ArrayList<>(filtered).subList(start, end);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }
}
