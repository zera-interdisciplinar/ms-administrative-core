package com.zera.ms_administrative_core.infrastructure.legacysync;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

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
