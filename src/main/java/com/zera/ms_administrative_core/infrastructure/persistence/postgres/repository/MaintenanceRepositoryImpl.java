package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.AuditContextBinder;

/**
 * Aciona as procedures de manutencao.
 *
 * <p>ARMADILHA QUE CUSTOU TEMPO: a forma "canonica" de JDBC para isto seria
 * {@code CallableStatement} com {@code {call sp(?, ?)}} e {@code registerOutParameter}. Nao
 * funciona no Postgres. O driver traduz a sintaxe de escape {@code {call ...}} para
 * {@code SELECT * FROM sp(?)} -- ou seja, trata o alvo como FUNCTION e descarta os parametros de
 * saida. O erro que aparece e enganoso:
 *
 * <pre>ERROR: function sp_fechar_alertas_obsoletos(integer) does not exist</pre>
 *
 * e manda "adicionar cast", quando o problema e que uma PROCEDURE nunca foi procurada. Existe a
 * propriedade de conexao {@code escapeSyntaxCallMode=call} que corrige isso, mas ela mudaria o
 * comportamento do DataSource inteiro para resolver tres metodos.
 *
 * <p>O caminho usado aqui e o {@code CALL} nativo: o Postgres devolve os valores {@code INOUT} como
 * UMA LINHA de resultado, entao basta le-la. Os literais {@code NULL} nas posicoes de saida sao o
 * valor de entrada dos INOUT, que a procedure sobrescreve.
 *
 * <p>Nao ha entidade JPA envolvida: procedure nao tem tabela.
 *
 * <p>O bind de auditoria acontece ANTES de cada CALL, e nao e detalhe: {@code
 * sp_fechar_alertas_obsoletos} faz um UPDATE em massa em {@code alert}, e a trigger de auditoria
 * grava uma linha POR alerta afetado. Sem publicar o autor, milhares de linhas de auditoria diriam
 * "nao partiu de uma pessoa" -- justamente na operacao em que "quem fez isso?" mais importa.
 */
@Repository
public class MaintenanceRepositoryImpl implements MaintenanceRepository {

    private final JdbcTemplate jdbc;
    private final AuditContextBinder auditContext;

    public MaintenanceRepositoryImpl(JdbcTemplate jdbc, AuditContextBinder auditContext) {
        this.jdbc = jdbc;
        this.auditContext = auditContext;
    }

    @Override
    @Transactional
    public int closeStaleAlerts(int dias) {
        auditContext.bindCurrentUser();
        Integer afetados = jdbc.queryForObject(
                "CALL sp_fechar_alertas_obsoletos(?, NULL)", Integer.class, dias);
        return afetados == null ? 0 : afetados;
    }

    @Override
    @Transactional
    public TokenCleanup revokeExpiredTokens(int diasRetencao) {
        auditContext.bindCurrentUser();
        Map<String, Object> saida = jdbc.queryForMap(
                "CALL sp_revogar_tokens_expirados(?, NULL, NULL)", diasRetencao);
        return new TokenCleanup(inteiro(saida, "p_revogados"), inteiro(saida, "p_removidos"));
    }

    @Override
    @Transactional
    public int consolidateDau(LocalDate de, LocalDate ate) {
        auditContext.bindCurrentUser();
        Integer dias = jdbc.queryForObject(
                "CALL sp_consolidar_dau(?, ?, NULL)", Integer.class, de, ate);
        return dias == null ? 0 : dias;
    }

    private int inteiro(Map<String, Object> linha, String coluna) {
        Object valor = linha.get(coluna);
        return valor == null ? 0 : ((Number) valor).intValue();
    }
}
