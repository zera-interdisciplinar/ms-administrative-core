package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova a trilha de auditoria da V10 contra Postgres real.
 *
 * <p>{@code @Transactional} nao e so isolamento: o parametro {@code zera.app_user} e
 * {@code SET LOCAL}, entao ele SO existe dentro de uma transacao. Testar a propagacao do usuario
 * exige que a escrita e o SET compartilhem a transacao -- exatamente a condicao que o adaptador de
 * producao garante com {@code @Transactional}.
 */
@Transactional
class AuditTriggerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    @DisplayName("INSERT deve gravar dados_novos e nenhum dados_antigos")
    void shouldAuditInsert() {
        Fixture f = novaHierarquia();

        Map<String, Object> log = ultimaAuditoria("audit_log_user_account", f.gestorId());

        assertThat(log.get("operacao")).isEqualTo("INSERT");
        assertThat(log.get("tabela")).isEqualTo("user_account");
        assertThat(log.get("dados_antigos")).isNull();
        assertThat(log.get("dados_novos")).isNotNull();
    }

    @Test
    @DisplayName("UPDATE deve gravar OLD e NEW, permitindo ver o que mudou")
    void shouldAuditUpdateWithOldAndNew() {
        Fixture f = novaHierarquia();

        jdbc.update("UPDATE user_account SET name = 'Nome Novo' WHERE id = ?", f.gestorId());

        Map<String, Object> log = jdbc.queryForMap("""
                SELECT operacao, dados_antigos ->> 'name' AS nome_antigo,
                       dados_novos ->> 'name' AS nome_novo
                  FROM audit_log_user_account
                 WHERE registro_id = ? AND operacao = 'UPDATE'
                 ORDER BY id DESC LIMIT 1
                """, f.gestorId().toString());

        assertThat(log.get("operacao")).isEqualTo("UPDATE");
        assertThat(log.get("nome_antigo")).isNotEqualTo("Nome Novo");
        assertThat(log.get("nome_novo")).isEqualTo("Nome Novo");
    }

    @Test
    @DisplayName("DELETE deve gravar dados_antigos e nenhum dados_novos")
    void shouldAuditDelete() {
        Fixture f = novaHierarquia();
        UUID funcionario = UUID.randomUUID();
        inserirUsuario(funcionario, f.unidadeId(), f.gestorId(), "EMPLOYEE", "ACTIVE");

        jdbc.update("DELETE FROM user_account WHERE id = ?", funcionario);

        Map<String, Object> log = jdbc.queryForMap("""
                SELECT operacao, dados_antigos IS NOT NULL AS tem_antigos,
                       dados_novos IS NULL AS novos_nulo
                  FROM audit_log_user_account
                 WHERE registro_id = ? AND operacao = 'DELETE'
                 ORDER BY id DESC LIMIT 1
                """, funcionario.toString());

        assertThat(log.get("operacao")).isEqualTo("DELETE");
        assertThat(log.get("tem_antigos")).isEqualTo(true);
        assertThat(log.get("novos_nulo")).isEqualTo(true);
    }

    /**
     * Auditoria que guarda hash de senha e um vazamento com nome bonito. Este e o teste que impede
     * alguem de "simplificar" fn_mascarar_sensiveis no futuro.
     */
    @Test
    @DisplayName("Nao deve gravar hash de senha no payload de auditoria")
    void shouldMaskPassword() {
        Fixture f = novaHierarquia();

        // jsonb_exists() e nao o operador `?`: num PreparedStatement o `?` do JSONB e consumido
        // como placeholder de parametro, e o erro que sai ("A result was returned when none was
        // expected" / contagem de parametros) nao aponta para o JSON em nada.
        Map<String, Object> log = jdbc.queryForMap("""
                SELECT jsonb_exists(dados_novos, 'password') AS tem_senha,
                       jsonb_exists(dados_novos, 'email')    AS tem_email,
                       jsonb_exists(dados_novos, 'name')     AS tem_nome
                  FROM audit_log_user_account
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, f.gestorId().toString());

        assertThat(log.get("tem_senha")).isEqualTo(false);
        // O resto do payload precisa continuar la: mascarar tudo tornaria a auditoria inutil.
        assertThat(log.get("tem_email")).isEqualTo(true);
        assertThat(log.get("tem_nome")).isEqualTo(true);
    }

    @Test
    @DisplayName("CURRENT_USER deve ser gravado como usuario_banco")
    void shouldRecordDatabaseUser() {
        Fixture f = novaHierarquia();

        String usuarioBanco = jdbc.queryForObject("""
                SELECT usuario_banco FROM audit_log_user_account
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, String.class, f.gestorId().toString());

        assertThat(usuarioBanco).isNotBlank().isEqualTo(
                jdbc.queryForObject("SELECT CURRENT_USER", String.class));
    }

    @Test
    @DisplayName("SET LOCAL zera.app_user deve chegar na auditoria como usuario_app")
    void shouldRecordApplicationUser() {
        UUID autor = UUID.randomUUID();
        jdbc.queryForObject("SELECT set_config('zera.app_user', ?, true)", String.class,
                autor.toString());

        Fixture f = novaHierarquia();

        UUID usuarioApp = jdbc.queryForObject("""
                SELECT usuario_app FROM audit_log_user_account
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, UUID.class, f.gestorId().toString());

        assertThat(usuarioApp).isEqualTo(autor);
    }

    /** Um parametro de sessao corrompido nao pode derrubar a escrita que esta sendo auditada. */
    @Test
    @DisplayName("app_user invalido deve cair para nulo sem quebrar a escrita")
    void shouldTolerateInvalidApplicationUser() {
        jdbc.queryForObject("SELECT set_config('zera.app_user', 'nao-e-uuid', true)", String.class);

        Fixture f = novaHierarquia();

        Object usuarioApp = jdbc.queryForMap("""
                SELECT usuario_app FROM audit_log_user_account
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, f.gestorId().toString()).get("usuario_app");

        assertThat(usuarioApp).isNull();
    }

    /**
     * HERANCA: cada tabela auditada grava na propria filha, e o pai continua vazio. Ler pelo pai ve
     * tudo (uniao da heranca); ler pela filha ve so uma tabela.
     */
    @Test
    @DisplayName("Auditoria deve ir para a tabela filha, nunca para o pai")
    void shouldWriteToInheritedChildTable() {
        Fixture f = novaHierarquia();

        Integer noPaiDireto = jdbc.queryForObject(
                "SELECT count(*) FROM ONLY audit_log", Integer.class);
        Integer naFilha = jdbc.queryForObject(
                "SELECT count(*) FROM ONLY audit_log_user_account WHERE registro_id = ?",
                Integer.class, f.gestorId().toString());
        Integer peloPai = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE registro_id = ?",
                Integer.class, f.gestorId().toString());

        assertThat(noPaiDireto).isZero();
        assertThat(naFilha).isEqualTo(1);
        assertThat(peloPai).isEqualTo(1);
    }

    @Test
    @DisplayName("Alerta e organizacao tambem devem ser auditados")
    void shouldAuditAlertAndOrganization() {
        Fixture f = novaHierarquia();
        UUID alerta = inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                java.time.LocalDateTime.now().withNano(0));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM ONLY audit_log_alert WHERE registro_id = ?",
                Integer.class, alerta.toString())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM ONLY audit_log_organization WHERE registro_id = ?",
                Integer.class, f.organizacaoId().toString())).isEqualTo(1);
    }

    /** O CHECK da V15 e o que permite ao planner podar filhas; tambem protege a integridade. */
    @Test
    @DisplayName("CHECK das filhas deve impedir gravar linha de outra tabela")
    void shouldRejectWrongTableInChild() {
        List<Map<String, Object>> constraints = jdbc.queryForList("""
                SELECT conname FROM pg_constraint
                 WHERE conrelid = 'audit_log_alert'::regclass AND contype = 'c'
                """);

        assertThat(constraints).isNotEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO audit_log_alert (tabela, operacao, usuario_banco, ocorrido_em)
                VALUES ('user_account', 'INSERT', 'x', LOCALTIMESTAMP(0))
                """)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private Map<String, Object> ultimaAuditoria(String tabela, UUID registroId) {
        return jdbc.queryForMap("SELECT * FROM " + tabela
                + " WHERE registro_id = ? ORDER BY id DESC LIMIT 1", registroId.toString());
    }
}
