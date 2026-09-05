package com.zera.ms_administrative_core.infrastructure.persistence.postgres.bootstrap;

import java.util.Optional;
import java.util.function.UnaryOperator;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

/**
 * Parametros do MANAGER inicial semeado pela migration {@code V8}. Vem de variaveis de ambiente
 * {@code BOOTSTRAP_ADMIN_*}. O seed so acontece quando email, senha e CNPJ da organizacao estao
 * presentes.
 */
public record BootstrapAdminSettings(
        String adminName,
        String adminEmail,
        String rawPassword,
        String organizationName,
        String organizationEmail,
        String organizationCnpj,
        String organizationPlan,
        String unitName
) {

    /**
     * @param env resolvedor de variaveis de ambiente (ex.: {@code System::getenv})
     * @return os parametros, ou vazio quando o trio obrigatorio
     *         ({@code BOOTSTRAP_ADMIN_EMAIL}, {@code BOOTSTRAP_ADMIN_PASSWORD},
     *         {@code BOOTSTRAP_ADMIN_ORG_CNPJ}) nao esta completo
     * @throws IllegalArgumentException se o email for invalido
     * @throws com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException
     *         se o CNPJ for invalido
     */
    public static Optional<BootstrapAdminSettings> fromEnv(UnaryOperator<String> env) {
        String adminEmail = trimToNull(env.apply("BOOTSTRAP_ADMIN_EMAIL"));
        String rawPassword = trimToNull(env.apply("BOOTSTRAP_ADMIN_PASSWORD"));
        String cnpj = trimToNull(env.apply("BOOTSTRAP_ADMIN_ORG_CNPJ"));

        if (adminEmail == null || rawPassword == null || cnpj == null) {
            return Optional.empty();
        }

        String normalizedEmail = new Email(adminEmail).value();
        String normalizedCnpj = new Cnpj(cnpj).value();
        String orgEmail = trimToNull(env.apply("BOOTSTRAP_ADMIN_ORG_EMAIL"));

        return Optional.of(new BootstrapAdminSettings(
                orElse(trimToNull(env.apply("BOOTSTRAP_ADMIN_NAME")), "Administrador"),
                normalizedEmail,
                rawPassword,
                orElse(trimToNull(env.apply("BOOTSTRAP_ADMIN_ORG_NAME")), "Organizacao Padrao"),
                orgEmail == null ? normalizedEmail : new Email(orgEmail).value(),
                normalizedCnpj,
                orElse(trimToNull(env.apply("BOOTSTRAP_ADMIN_ORG_PLAN")), "FREE"),
                orElse(trimToNull(env.apply("BOOTSTRAP_ADMIN_UNIT_NAME")), "Matriz")));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String orElse(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
