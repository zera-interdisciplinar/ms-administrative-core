package com.zera.ms_administrative_core.core.usecase.registerUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;

public record RegisterUserCommand(
    String name,
    Role role,
    String rawPassword,
    String email,
    UUID unitId
) {}
