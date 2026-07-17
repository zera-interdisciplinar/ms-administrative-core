package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpa, UUID> {
    Optional<UserJpa> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
    SELECT u FROM UserJpa u
    WHERE (:role IS NULL OR u.role = :role)
    AND (:status IS NULL OR u.status = :status)
""")
    Page<UserJpa> findAllByRoleAndStatus(
            @Param("role") Role role,
            @Param("status") Status status,
            Pageable pageable
    );
}
