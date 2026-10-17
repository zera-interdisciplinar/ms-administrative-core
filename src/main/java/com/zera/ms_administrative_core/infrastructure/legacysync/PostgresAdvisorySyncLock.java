package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Lock por advisory lock de TRANSACAO do Postgres, no banco do proprio servico (que todas as
 * replicas ja compartilham): nao precisa de tabela, migracao nem dependencia.
 *
 * <p>E de transacao ({@code pg_try_advisory_xact_lock}) e nao de sessao de proposito: o Hikari
 * DEVOLVE a conexao ao pool em vez de fecha-la, entao um lock de sessao cujo {@code unlock}
 * falhasse ficaria preso naquela sessao para sempre e nenhum pod voltaria a sincronizar. O de
 * transacao morre no commit/rollback e, se o pod cair, junto com a conexao.
 *
 * <p>A rodada segura UMA conexao do pool principal enquanto roda, mas nao executa mais nada nela
 * depois do SELECT do lock, entao nao trava linha nem segura snapshot.
 */
public class PostgresAdvisorySyncLock implements SyncLock {

    // "zeraSync" em ASCII: constante legivel, sem depender de hashtext (funcao nao documentada)
    private static final long KEY = 0x7A65726153796E63L;

    private final DataSource dataSource;

    public PostgresAdvisorySyncLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> Optional<T> tryRun(Supplier<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!tryAcquire(connection)) {
                    connection.rollback();
                    return Optional.empty();
                }
                T result = work.get();
                connection.commit();
                return Optional.of(result);
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            // nao e CannotGetJdbcConnectionException de proposito: o job usa esse tipo para dizer
            // "sem conexao com o legado", e aqui o banco que falhou e o nosso
            throw new DataAccessResourceFailureException("Falha ao obter o lock do sync legado", e);
        }
    }

    private static boolean tryAcquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_xact_lock(?)")) {
            statement.setLong(1, KEY);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static void rollbackQuietly(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException suppressed) {
            original.addSuppressed(suppressed);
        }
    }
}
