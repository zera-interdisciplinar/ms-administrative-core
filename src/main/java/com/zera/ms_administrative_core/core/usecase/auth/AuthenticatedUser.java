package com.zera.ms_administrative_core.core.usecase.auth;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Role;

/** Identidade minima de um usuario autenticado, usada para montar os claims do access token. */
public record AuthenticatedUser(UUID userId, String email, Role role) {
}
