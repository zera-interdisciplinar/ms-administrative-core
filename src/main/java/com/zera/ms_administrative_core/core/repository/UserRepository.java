package com.zera.ms_administrative_core.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public interface UserRepository {
    void save(User user);
    void delete(UUID id);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(Email email);
    /** Filtros nulos nao restringem; {@code unitId} atende quem precisa dos usuarios de uma unidade. */
    List<User> findAll(Role role, Status status, UUID managerId, UUID unitId, int page, int size);
    boolean existsByEmail(Email email);
    List<ManagerEmployeeCount> countEmployeesGroupedByManager();
}