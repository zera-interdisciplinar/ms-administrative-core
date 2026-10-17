package com.zera.ms_administrative_core.infrastructure.legacysync;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "legacy-sync.enabled=true",
        "legacy-sync.interval=PT1H",
        "legacy-sync.initial-delay=PT1H",
        "legacy-sync.db.url=jdbc:postgresql://localhost:1/legacy",
        "legacy-sync.db.user=legacy",
        "legacy-sync.db.password=legacy"
})
class LegacySyncWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Should register the sync beans when enabled")
    void shouldRegisterSyncBeans() {
        assertThat(context.getBeansOfType(LegacySyncJob.class)).hasSize(1);
        assertThat(context.getBeansOfType(LegacySyncService.class)).hasSize(1);
        assertThat(context.getBeansOfType(LegacyReader.class)).hasSize(1);
        assertThat(context.getBeansOfType(LegacyWriter.class)).hasSize(1);
        assertThat(context.getBeansOfType(SyncLock.class)).hasSize(1);
    }

    // O src/test/resources/application.properties sombreia o de producao, entao o contexto de teste
    // nunca enxerga esta propriedade: o unico jeito de guarda-la e ler o arquivo de producao.
    @Test
    @DisplayName("Should configure more than one scheduler thread so a slow sync does not starve other jobs")
    void shouldConfigureMoreThanOneSchedulerThread() throws Exception {
        Properties production = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            production.load(reader);
        }

        assertThat(Integer.parseInt(production.getProperty("spring.task.scheduling.pool.size")))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should keep the main DataSource as the only DataSource bean")
    void shouldKeepMainDataSourceUntouched() throws Exception {
        assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);

        DataSource main = context.getBean(DataSource.class);
        try (Connection connection = main.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:");
        }
    }
}
