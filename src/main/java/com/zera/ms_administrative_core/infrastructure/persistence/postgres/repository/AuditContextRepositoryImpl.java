package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.repository.AuditContextRepository;

/**
 * Publica o usuario da aplicacao como parametro de sessao, para a trigger de auditoria ler.
 *
 * <p>{@code set_config(..., true)} e o equivalente de {@code SET LOCAL}: vale so na transacao
 * corrente e o banco o descarta no commit ou rollback. Isso importa num pool de conexoes -- um
 * {@code SET} de sessao ficaria grudado na conexao e a proxima requisicao, de outro usuario,
 * herdaria a identidade errada na auditoria.
 *
 * <p>Se nao houver transacao em andamento, o {@code SET LOCAL} nao alcanca a escrita seguinte (o
 * JdbcTemplate usaria outra conexao) e a auditoria grava {@code usuario_app} nulo. Por isso os
 * pontos de escrita que chamam este metodo sao {@code @Transactional}.
 */
@Repository
public class AuditContextRepositoryImpl implements AuditContextRepository {

    private final JdbcTemplate jdbc;

    public AuditContextRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bindUser(UUID usuarioId) {
        jdbc.queryForObject("SELECT set_config('zera.app_user', ?, true)", String.class,
                usuarioId == null ? "" : usuarioId.toString());
    }
}
