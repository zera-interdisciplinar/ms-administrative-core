package com.zera.ms_administrative_core.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifica o chaveamento do agendador de manutencao.
 *
 * <p>O caso DESLIGADO importa mais que o ligado: o padrao e nao agendar, porque com mais de uma
 * replica todas rodariam a mesma procedure no mesmo minuto. Um agendador que se liga sozinho por
 * descuido de configuracao seria descoberto em producao, nao aqui.
 */
class MaintenanceSchedulerWiringTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "zera.maintenance.enabled=true",
            "zera.maintenance.interval=PT24H",
            "zera.maintenance.initial-delay=PT24H"
    })
    class Habilitado {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Deve registrar o job quando zera.maintenance.enabled=true")
        void shouldRegisterJobWhenEnabled() {
            assertThat(context.getBeansOfType(MaintenanceJob.class)).hasSize(1);
            assertThat(context.getBean(MaintenanceProperties.class)).isNotNull();
        }

        /** Com o intervalo de 24h do teste, o job nao dispara durante a suite. */
        @Test
        @DisplayName("Job registrado deve estar utilizavel sem lancar na construcao")
        void shouldExposeUsableJob() {
            assertThat(context.getBean(MaintenanceJob.class)).isNotNull();
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class DesligadoPorPadrao {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Nao deve registrar o job sem habilitacao explicita")
        void shouldNotRegisterJobByDefault() {
            assertThat(context.getBeansOfType(MaintenanceJob.class)).isEmpty();
        }
    }
}
