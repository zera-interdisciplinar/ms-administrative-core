package com.zera.ms_administrative_core.infrastructure.legacysync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Verifica o protocolo JDBC (quando commita, quando faz rollback, quando devolve a conexao). Que o
 * Postgres de fato nega o lock a uma segunda conexao e provado em
 * {@code PostgresAdvisorySyncLockIntegrationTest}, que precisa de Docker.
 */
@ExtendWith(MockitoExtension.class)
class PostgresAdvisorySyncLockTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private PostgresAdvisorySyncLock lock;

    @BeforeEach
    void setUp() throws SQLException {
        lock = new PostgresAdvisorySyncLock(dataSource);
        // lenient: o teste de falha ao obter a conexao nao chega a usar nada alem do DataSource
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(statement);
        lenient().when(statement.executeQuery()).thenReturn(resultSet);
        lenient().when(resultSet.next()).thenReturn(true);
    }

    @Test
    @DisplayName("Should run the work and commit, which releases the lock, when the lock is granted")
    void shouldRunAndCommitWhenGranted() throws SQLException {
        when(resultSet.getBoolean(1)).thenReturn(true);

        Optional<String> result = lock.tryRun(() -> "done");

        assertThat(result).contains("done");
        InOrder order = inOrder(connection, statement);
        order.verify(connection).setAutoCommit(false);
        order.verify(statement).setLong(org.mockito.ArgumentMatchers.eq(1), anyLong());
        order.verify(connection).commit();
        verify(connection).close();
    }

    @Test
    @DisplayName("Should not run the work, and should end the transaction, when another pod holds the lock")
    void shouldSkipWorkWhenLockIsHeld() throws SQLException {
        when(resultSet.getBoolean(1)).thenReturn(false);
        AtomicInteger runs = new AtomicInteger();

        Optional<String> result = lock.tryRun(() -> {
            runs.incrementAndGet();
            return "done";
        });

        assertThat(result).isEmpty();
        assertThat(runs).hasValue(0);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    @DisplayName("Should roll back, release the connection and rethrow when the work fails")
    void shouldRollbackWhenWorkFails() throws SQLException {
        when(resultSet.getBoolean(1)).thenReturn(true);

        assertThatThrownBy(() -> lock.tryRun(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    @DisplayName("Should report a failure to reach our own database without calling it a legacy connection failure")
    void shouldWrapFailureToGetConnection() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> lock.tryRun(() -> "x"))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .isNotInstanceOf(org.springframework.jdbc.CannotGetJdbcConnectionException.class);
    }
}
