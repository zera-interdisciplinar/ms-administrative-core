package com.zera.ms_administrative_core.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException;

class BootstrapAdminSettingsTest {

    private static final String VALID_CNPJ = "11222333000181";

    private static Map<String, String> baseEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("BOOTSTRAP_ADMIN_EMAIL", "Admin@Zera.com ");
        env.put("BOOTSTRAP_ADMIN_PASSWORD", "s3nh4-forte");
        env.put("BOOTSTRAP_ADMIN_ORG_CNPJ", "11.222.333/0001-81");
        return env;
    }

    @Test
    void emptyWhenRequiredTrioIsIncomplete() {
        Map<String, String> env = baseEnv();
        env.remove("BOOTSTRAP_ADMIN_ORG_CNPJ");

        assertThat(BootstrapAdminSettings.fromEnv(env::get)).isEmpty();
        assertThat(BootstrapAdminSettings.fromEnv(k -> null)).isEmpty();
    }

    @Test
    void blankValuesCountAsMissing() {
        Map<String, String> env = baseEnv();
        env.put("BOOTSTRAP_ADMIN_PASSWORD", "   ");

        assertThat(BootstrapAdminSettings.fromEnv(env::get)).isEmpty();
    }

    @Test
    void normalizesEmailAndCnpjAndAppliesDefaults() {
        BootstrapAdminSettings settings = BootstrapAdminSettings.fromEnv(baseEnv()::get).orElseThrow();

        assertThat(settings.adminEmail()).isEqualTo("admin@zera.com");
        assertThat(settings.organizationCnpj()).isEqualTo(VALID_CNPJ);
        assertThat(settings.organizationEmail()).isEqualTo("admin@zera.com");
        assertThat(settings.adminName()).isEqualTo("Administrador");
        assertThat(settings.organizationName()).isEqualTo("Organizacao Padrao");
        assertThat(settings.organizationPlan()).isEqualTo("FREE");
        assertThat(settings.unitName()).isEqualTo("Matriz");
        assertThat(settings.rawPassword()).isEqualTo("s3nh4-forte");
    }

    @Test
    void honoursOptionalOverrides() {
        Map<String, String> env = baseEnv();
        env.put("BOOTSTRAP_ADMIN_NAME", "Fulano");
        env.put("BOOTSTRAP_ADMIN_ORG_NAME", "Zera LTDA");
        env.put("BOOTSTRAP_ADMIN_ORG_EMAIL", "contato@zera.com");
        env.put("BOOTSTRAP_ADMIN_ORG_PLAN", "PRO");
        env.put("BOOTSTRAP_ADMIN_UNIT_NAME", "Filial Sul");

        BootstrapAdminSettings settings = BootstrapAdminSettings.fromEnv(env::get).orElseThrow();

        assertThat(settings.adminName()).isEqualTo("Fulano");
        assertThat(settings.organizationName()).isEqualTo("Zera LTDA");
        assertThat(settings.organizationEmail()).isEqualTo("contato@zera.com");
        assertThat(settings.organizationPlan()).isEqualTo("PRO");
        assertThat(settings.unitName()).isEqualTo("Filial Sul");
    }

    @Test
    void rejectsInvalidCnpj() {
        Map<String, String> env = baseEnv();
        env.put("BOOTSTRAP_ADMIN_ORG_CNPJ", "12345678000100");

        assertThatThrownBy(() -> BootstrapAdminSettings.fromEnv(env::get))
                .isInstanceOf(InvalidCnpjException.class);
    }

    @Test
    void rejectsInvalidEmail() {
        Map<String, String> env = baseEnv();
        env.put("BOOTSTRAP_ADMIN_EMAIL", "not-an-email");

        assertThatThrownBy(() -> BootstrapAdminSettings.fromEnv(env::get))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
