package com.zera.ms_administrative_core.infrastructure.legacysync;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "legacy-sync.enabled", havingValue = "true")
class LegacySyncConfig {

    // Nao expor o DataSource do legado como bean: isso desligaria o DataSource principal do Spring Boot.
    @Bean
    LegacyReader legacyReader(@Value("${legacy-sync.db.url}") String url,
            @Value("${legacy-sync.db.user}") String user,
            @Value("${legacy-sync.db.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("legacy-sync");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(2);
        config.setReadOnly(true);
        // sem isso, um legado que descarta pacotes trava a unica thread do agendador (em segundos)
        config.addDataSourceProperty("connectTimeout", "10");
        config.addDataSourceProperty("socketTimeout", "60");
        // legado fora do ar nao pode impedir o servico de subir
        config.setInitializationFailTimeout(-1);
        return new JdbcLegacyReader(new HikariDataSource(config));
    }

    @Bean
    LegacySyncService legacySyncService(LegacyReader reader, OrganizationRepository organizations,
            UnitRepository units, UserRepository users, TelephoneRepository telephones) {
        return new LegacySyncService(reader, organizations, units, users, telephones);
    }

    @Bean
    LegacySyncJob legacySyncJob(LegacySyncService service) {
        return new LegacySyncJob(service);
    }
}
