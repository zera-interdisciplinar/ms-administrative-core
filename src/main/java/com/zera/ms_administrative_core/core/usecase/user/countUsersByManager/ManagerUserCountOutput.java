package com.zera.ms_administrative_core.core.usecase.user.countUsersByManager;

import com.zera.ms_administrative_core.core.repository.ManagerEmployeeCount;

import java.util.UUID;

public record ManagerUserCountOutput(UUID managerId, long count) {
    public static ManagerUserCountOutput from(ManagerEmployeeCount count) {
        return new ManagerUserCountOutput(count.managerId(), count.count());
    }
}
