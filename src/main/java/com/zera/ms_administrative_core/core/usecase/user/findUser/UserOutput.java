package com.zera.ms_administrative_core.core.usecase.user.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserOutput(
        UUID userId,
        String name,
        Email email,
        Role role,
        Status status,
        UUID unitId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID managerId
) {
    public static UserOutput from(User user) {
        return new UserOutput(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.role(),
                user.getStatus(),
                user.getUnitId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user instanceof Employee employee ? employee.getManagerId() : null
        );
    }
}