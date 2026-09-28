package com.zera.ms_administrative_core.core.usecase.user.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.util.List;
import java.util.UUID;

public interface FindAllUsers {
    List<UserOutput> execute(Role role, Status status, UUID managerId, UUID unitId, int page, int size);
}