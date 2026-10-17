package com.zera.ms_administrative_core.infrastructure.legacysync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class LegacySyncJobTest {

    @Mock
    private LegacySyncService service;

    private LegacySyncJob job;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        job = new LegacySyncJob(service);
        logger = (Logger) LoggerFactory.getLogger(LegacySyncJob.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("Should log the report together with the round duration")
    void shouldLogDurationOnSuccess() {
        when(service.syncAll()).thenReturn(new SyncReport(7, 2, 3));

        job.run();

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .matches("Legacy sync concluido: 7 gravados, 2 ignorados, 3 enviados ao legado em \\d+ms");
        });
    }

    @Test
    @DisplayName("Should log a connection failure as one line with the root cause and no stack trace")
    void shouldLogRootCauseOnlyWhenLegacyIsUnreachable() {
        Throwable root = new SocketTimeoutException("Connect timed out");
        when(service.syncAll()).thenThrow(new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection",
                new SQLTransientConnectionException("legacy-sync - Connection is not available", root)));

        job.run();

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .startsWith("Legacy sync falhou em ")
                    .contains("sem conexao com o legado")
                    .endsWith("java.net.SocketTimeoutException: Connect timed out");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    @DisplayName("Should keep the stack trace for any other failure")
    void shouldKeepStackTraceForUnexpectedFailure() {
        when(service.syncAll()).thenThrow(new IllegalStateException("boom"));

        job.run();

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).startsWith("Legacy sync falhou em ");
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
        });
    }
}
