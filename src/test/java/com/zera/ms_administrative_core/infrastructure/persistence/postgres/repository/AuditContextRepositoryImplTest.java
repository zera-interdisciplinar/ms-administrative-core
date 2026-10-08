package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AuditContextRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private AuditContextRepositoryImpl repository;

    /**
     * O terceiro argumento de set_config e `true` = escopo de transacao. Isso nao e detalhe de
     * estilo: com `false` o valor ficaria grudado na conexao do pool e a proxima requisicao, de
     * outro usuario, herdaria a identidade errada na auditoria.
     */
    @Test
    @DisplayName("Deve publicar o usuario com escopo de transacao (SET LOCAL)")
    void shouldBindUserAsTransactionLocal() {
        UUID usuario = UUID.randomUUID();

        repository.bindUser(usuario);

        verify(jdbc).queryForObject(
                eq("SELECT set_config('zera.app_user', ?, true)"), eq(String.class),
                eq(usuario.toString()));
    }

    @Test
    @DisplayName("Deve publicar string vazia quando nao ha usuario, em vez de 'null'")
    void shouldBindEmptyWhenNoUser() {
        repository.bindUser(null);

        verify(jdbc).queryForObject(
                eq("SELECT set_config('zera.app_user', ?, true)"), eq(String.class), eq(""));
    }
}
