package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.domain.valueobject.AccessOrigin;
import com.zera.ms_administrative_core.core.repository.AccessContextRepository;

/**
 * Publica a origem da sessao como parametro de transacao, lido pelo trigger
 * {@code fn_registrar_acesso} (V12) via {@code current_setting('zera.access_origin', true)}.
 *
 * <p>{@code set_config(..., true)} vale so ate o commit, pelo mesmo motivo do binder de auditoria:
 * num pool de conexoes, um valor de sessao vazaria para a requisicao seguinte.
 *
 * <p>Este metodo precisa ser chamado dentro de uma transacao que tambem faca o INSERT em
 * refresh_token. Quem garante isso e o {@code @Transactional} em {@code LoginImpl} e
 * {@code RefreshSessionImpl}: sem ele, o SET LOCAL roda numa conexao e o INSERT em outra, e o
 * trigger nao enxerga a origem.
 */
@Repository
public class AccessContextRepositoryImpl implements AccessContextRepository {

    private final JdbcTemplate jdbc;

    public AccessContextRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bindOrigin(AccessOrigin origin) {
        jdbc.queryForObject("SELECT set_config('zera.access_origin', ?, true)", String.class,
                origin.name());
    }
}
