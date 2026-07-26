package com.zera.ms_administrative_core.core.usecase.user.registerUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public record RegisterUserOutput(
    UUID userId,
    String name,
    Email email,
    Role role
) {}
