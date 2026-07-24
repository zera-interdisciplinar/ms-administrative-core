package com.zera.ms_administrative_core.core.usecase.user.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.util.List;

public interface FindAllUsers {
    List<UserOutput> execute(Role role, Status status, int page, int size);
}