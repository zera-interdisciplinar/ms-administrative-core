package com.zera.ms_administrative_core.core.usecase.register;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;

public record RegisterUserCommand(
    String name,
    Role role,
    String rawPassword,
    String email,
    UUID unityId
) {}
