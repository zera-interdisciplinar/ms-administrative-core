package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Roda todas as migrations Flyway contra um Postgres real (Testcontainers). O H2 dos testes
 * unitarios nao exercita indices parciais, {@code gen_random_uuid()} nem os {@code CHECK} — este
 * teste sim, fechando o gap de schema-drift entre o que os testes veem e o que produz.
 *
 * <p>Passou a estender {@link AbstractPostgresIntegrationTest} para compartilhar UM unico container
 * com o resto da suite de integracao, em vez de subir um Postgres so para esta classe.
 *
 * <p>Pulado onde nao ha Docker; roda no CI.
 */
class FlywayPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    Flyway flyway;

    @Test
    void allMigrationsApplyCleanlyOnRealPostgres() {
        var applied = flyway.info().applied();

        assertThat(applied).hasSizeGreaterThanOrEqualTo(17);
        assertThat(applied).allMatch(m -> m.getState() == MigrationState.SUCCESS);
    }

    @Test
    void postgresSpecificObjectsExist() {
        // indice parcial da V5 (dedup de alerta OPEN) — H2 nao suporta indice com WHERE
        Integer alertIdx = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'ux_alert_open_rule_event'",
                Integer.class);
        assertThat(alertIdx).isEqualTo(1);

        // indice parcial da V6 (convite PENDING unico por codigo)
        Integer inviteIdx = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'ux_invitation_pending_code'",
                Integer.class);
        assertThat(inviteIdx).isEqualTo(1);

        // tabela da V7
        Integer refreshTable = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'refresh_token'",
                Integer.class);
        assertThat(refreshTable).isEqualTo(1);
    }

    /**
     * Objetos da V10-V17. Cada um esta aqui porque o H2 dos testes unitarios nao o suporta: sem
     * assercao explicita, uma migracao que deixasse de aplicar passaria despercebida.
     */
    @Test
    void functionsAndProceduresExist() {
        assertThat(existeRotina("fn_validar_cnpj", 'f')).isTrue();
        assertThat(existeRotina("fn_tamanho_equipe", 'f')).isTrue();
        assertThat(existeRotina("fn_indice_saude_unidade", 'f')).isTrue();
        assertThat(existeRotina("fn_auditoria", 'f')).isTrue();
        assertThat(existeRotina("fn_mascarar_sensiveis", 'f')).isTrue();
        assertThat(existeRotina("fn_registrar_acesso", 'f')).isTrue();
        assertThat(existeRotina("fn_catalogo_divergencia", 'f')).isTrue();

        assertThat(existeRotina("sp_fechar_alertas_obsoletos", 'p')).isTrue();
        assertThat(existeRotina("sp_revogar_tokens_expirados", 'p')).isTrue();
        assertThat(existeRotina("sp_consolidar_dau", 'p')).isTrue();
    }

    @Test
    void auditTriggersExist() {
        for (String trigger : new String[] {
                "trg_auditoria_user_account", "trg_auditoria_alert", "trg_auditoria_organization",
                "trg_registrar_acesso"}) {
            Integer total = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_trigger WHERE tgname = ? AND NOT tgisinternal",
                    Integer.class, trigger);
            assertThat(total).as("trigger %s", trigger).isEqualTo(1);
        }
    }

    /** Heranca de tabelas: o H2 nao tem INHERITS, entao so aqui isso e verificavel. */
    @Test
    void auditLogInheritanceExists() {
        Integer filhas = jdbc.queryForObject("""
                SELECT count(*) FROM pg_inherits i
                  JOIN pg_class pai ON pai.oid = i.inhparent
                 WHERE pai.relname = 'audit_log'
                """, Integer.class);

        assertThat(filhas).isEqualTo(3);
    }

    /** CHECK nas filhas: e o que permite ao planner podar a heranca (ver docs/otimizacao.md). */
    @Test
    void auditChildrenHaveDiscriminatingCheck() {
        for (String filha : new String[] {
                "audit_log_user_account", "audit_log_alert", "audit_log_organization"}) {
            Integer checks = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_constraint
                     WHERE conrelid = ?::regclass AND contype = 'c'
                    """, Integer.class, filha);
            assertThat(checks).as("CHECK de %s", filha).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void biSchemaAndViewsExist() {
        Integer schema = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'bi'",
                Integer.class);
        assertThat(schema).isEqualTo(1);

        for (String view : new String[] {
                "dim_tempo", "dim_organizacao", "dim_unidade", "dim_usuario",
                "fato_alerta", "fato_acesso",
                "vw_alertas_mensal_unidade", "vw_dau_diario", "vw_ranking_unidades",
                "vw_hierarquia_equipe"}) {
            Integer total = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_views WHERE schemaname = 'bi' AND viewname = ?",
                    Integer.class, view);
            assertThat(total).as("view bi.%s", view).isEqualTo(1);
        }
    }

    @Test
    void optimizationIndexesExist() {
        for (String indice : new String[] {
                "idx_alert_user_status_created", "idx_alert_unit_occurred",
                "idx_user_account_unit_role", "idx_user_account_unit_status",
                "idx_audit_alert_data", "idx_user_access_log_usuario_dia"}) {
            Integer total = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indice);
            assertThat(total).as("indice %s", indice).isEqualTo(1);
        }
    }

    /** Indice redundante removido na V15: se voltar, volta o custo de escrita sem ganho de leitura. */
    @Test
    void redundantIndexWasDropped() {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_alert_user_id'",
                Integer.class);

        assertThat(total).isZero();
    }

    @Test
    void accessRolesExist() {
        for (String role : new String[] {"zera_bi_leitor", "zera_auditor"}) {
            Integer total = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_roles WHERE rolname = ?", Integer.class, role);
            assertThat(total).as("role %s", role).isEqualTo(1);
        }
    }

    /**
     * A role de BI ve `bi` e NAO ve `public`. Uma view roda com os privilegios de quem a criou, e e
     * isso que permite a ferramenta de BI ler os numeros sem alcancar hash de senha.
     */
    @Test
    void biRoleReadsAnalyticsButNotOperationalTables() {
        assertThat(jdbc.queryForObject(
                "SELECT has_table_privilege('zera_bi_leitor', 'bi.fato_alerta', 'SELECT')",
                Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT has_table_privilege('zera_bi_leitor', 'public.user_account', 'SELECT')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT has_table_privilege('zera_bi_leitor', 'public.alert', 'SELECT')",
                Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT has_table_privilege('zera_bi_leitor', 'bi.fato_alerta', 'DELETE')",
                Boolean.class)).isFalse();
    }

    @Test
    void dataCatalogIsPopulated() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo_tabela", Integer.class))
                .isGreaterThanOrEqualTo(14);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo_coluna", Integer.class))
                .isGreaterThanOrEqualTo(100);
    }

    private boolean existeRotina(String nome, char tipo) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM pg_proc WHERE proname = ? AND prokind = ?",
                Integer.class, nome, String.valueOf(tipo));
        return total != null && total >= 1;
    }
}
