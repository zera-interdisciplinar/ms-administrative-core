package com.zera.ms_administrative_core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve extrair o id do usuario do principal (claim sub)")
    void shouldExtractUserId() {
        UUID usuario = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.toString(), "n/a", List.of()));

        assertThat(provider.currentUserId()).contains(usuario);
    }

    @Test
    @DisplayName("Deve devolver vazio sem autenticacao")
    void shouldReturnEmptyWhenAnonymous() {
        assertThat(provider.currentUserId()).isEmpty();
    }

    @Test
    @DisplayName("Deve devolver vazio quando nao autenticado")
    void shouldReturnEmptyWhenNotAuthenticated() {
        TestingAuthenticationToken token =
                new TestingAuthenticationToken(UUID.randomUUID().toString(), "n/a");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(provider.currentUserId()).isEmpty();
    }

    /** Token de servico: o `sub` e o id do cliente (ms-inventory), nao um UUID de usuario. */
    @Test
    @DisplayName("Deve devolver vazio para token de servico, cujo sub nao e UUID")
    void shouldReturnEmptyForServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ms-inventory", "n/a", List.of()));

        assertThat(provider.currentUserId()).isEmpty();
    }
}
