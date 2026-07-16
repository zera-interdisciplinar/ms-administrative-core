package com.zera.ms_administrative_core.core.domain;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public final class UserFactory {

    private UserFactory() {}

    public static User create(Role role, UUID id, String name, Email email,
                               HashedPassword password, Status status, UUID unitId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return switch (role) {
            case MANAGER  -> new Manager(id, name, email, password, status, unitId, now, now);
            case EMPLOYEE -> new Employee(id, name, email, password, status, unitId, now, now);
        };
    }
    
    public static User load(Role role, UUID id, String name, Email email,
                                     HashedPassword password, Status status, UUID unitId,
                                     java.time.LocalDateTime createdAt,
                                     java.time.LocalDateTime updatedAt) {
        return switch (role) {
            case MANAGER  -> new Manager(id, name, email, password, status, unitId, createdAt, updatedAt);
            case EMPLOYEE -> new Employee(id, name, email, password, status, unitId, createdAt, updatedAt);
        };
    }
}