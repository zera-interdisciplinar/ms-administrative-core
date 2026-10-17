package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zera.ms_administrative_core.infrastructure.legacysync.PostgresAdvisorySyncLock;

/**
 * Prova no Postgres de verdade o que o teste unitario nao pode: que uma SEGUNDA conexao nao obtem
 * o lock enquanto a primeira o segura, e que ele e solto no fim, inclusive quando o trabalho falha.
 *
 * <p>Nao escreve nada, por isso dispensa {@code @Transactional} (ver a regra em
 * {@link AbstractPostgresIntegrationTest}).
 */
class SyncLockPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should deny the lock to a second connection while the first one holds it")
    void shouldDenyLockToSecondConnection() {
        PostgresAdvisorySyncLock lock = new PostgresAdvisorySyncLock(dataSource);

        // o tryRun interno pega OUTRA conexao do pool, como faria outro pod
        Optional<Optional<String>> outer = lock.tryRun(() -> lock.tryRun(() -> "inner"));

        assertThat(outer).isPresent();
        assertThat(outer.get()).isEmpty();
    }

    @Test
    @DisplayName("Should release the lock after the work finishes")
    void shouldReleaseLockAfterWork() {
        PostgresAdvisorySyncLock lock = new PostgresAdvisorySyncLock(dataSource);

        assertThat(lock.tryRun(() -> "first")).contains("first");
        assertThat(lock.tryRun(() -> "second")).contains("second");
    }

    @Test
    @DisplayName("Should release the lock even when the work throws")
    void shouldReleaseLockWhenWorkThrows() {
        PostgresAdvisorySyncLock lock = new PostgresAdvisorySyncLock(dataSource);

        assertThatThrownBy(() -> lock.tryRun(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(lock.tryRun(() -> "after")).contains("after");
    }
}
