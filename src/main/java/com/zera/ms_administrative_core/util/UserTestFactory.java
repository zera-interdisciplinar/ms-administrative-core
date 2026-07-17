package com.zera.ms_administrative_core.util;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserTestFactory {

    public static Manager createManager() {
        return new Manager(
                UUID.randomUUID(),
                "João Silva",
                new Email("joao@empresa.com"),
                new HashedPassword("$2a$10$hashedpassword"),
                Status.ACTIVE,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static Employee createEmployee() {
        return new Employee(
                UUID.randomUUID(),
                "João Silva",
                new Email("joao@empresa.com"),
                new HashedPassword("$2a$10$hashedpassword"),
                Status.ACTIVE,
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}