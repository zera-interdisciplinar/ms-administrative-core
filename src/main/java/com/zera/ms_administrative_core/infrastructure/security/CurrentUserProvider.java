package com.zera.ms_administrative_core.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Descobre o usuario da requisicao corrente a partir do token.
 *
 * <p>O {@code principal} e o claim {@code sub}, que e o id do usuario. Nem toda requisicao tem um:
 * token de servico autentica um CLIENTE, nao uma pessoa, e job/migracao nao tem requisicao nenhuma.
 * Por isso o retorno e {@link Optional} -- ausencia e um caso normal, nao um erro.
 */
@Component
public class CurrentUserProvider {

    public Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException e) {
            // Token de servico: o `sub` e o id do cliente (ms-inventory), nao um UUID de usuario.
            return Optional.empty();
        }
    }
}
