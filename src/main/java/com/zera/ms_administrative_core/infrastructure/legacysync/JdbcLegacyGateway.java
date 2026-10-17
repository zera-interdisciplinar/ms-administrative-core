package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Unico ponto de acesso ao banco legado: leitura ({@link LegacyReader}) e escrita
 * ({@link LegacyWriter}) compartilham o mesmo pool.
 */
public class JdbcLegacyGateway implements LegacyReader, LegacyWriter, AutoCloseable {

    // assinatura mais recente da organizacao; LEFT JOIN para nao perder quem ainda nao tem assinatura
    private static final String ORGANIZACOES = """
            SELECT o.codigo, o.cnpj, o.nome, o.email, o.criado_em, o.atualizado_em,
                   p.nome AS plano, a.status AS status_assinatura
            FROM organizacao o
            LEFT JOIN LATERAL (
                SELECT s.cod_plano, s.status
                FROM assinatura s
                WHERE s.cod_organizacao = o.codigo
                ORDER BY s.data_inicio DESC NULLS LAST, s.codigo DESC
                LIMIT 1
            ) a ON true
            LEFT JOIN plano p ON p.codigo = a.cod_plano
            ORDER BY o.codigo
            """;

    private static final String UNIDADES = """
            SELECT codigo, cod_organizacao, criado_em, atualizado_em
            FROM unidade
            ORDER BY codigo
            """;

    private static final String GESTORES = """
            SELECT codigo, nome, email, senha, telefone, cod_unidade, criado_em, atualizado_em
            FROM gestor
            ORDER BY codigo
            """;

    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbc;

    public JdbcLegacyGateway(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Organizacao> organizacoes() {
        return jdbc.query(ORGANIZACOES, (rs, i) -> new Organizacao(
                rs.getInt("codigo"),
                rs.getString("cnpj"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getObject("criado_em", OffsetDateTime.class),
                rs.getObject("atualizado_em", OffsetDateTime.class),
                rs.getString("plano"),
                rs.getString("status_assinatura")));
    }

    @Override
    public List<Unidade> unidades() {
        return jdbc.query(UNIDADES, (rs, i) -> new Unidade(
                rs.getInt("codigo"),
                rs.getObject("cod_organizacao", Integer.class),
                rs.getObject("criado_em", OffsetDateTime.class),
                rs.getObject("atualizado_em", OffsetDateTime.class)));
    }

    @Override
    public List<Gestor> gestores() {
        return jdbc.query(GESTORES, (rs, i) -> new Gestor(
                rs.getInt("codigo"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha"),
                rs.getString("telefone"),
                rs.getObject("cod_unidade", Integer.class),
                rs.getObject("criado_em", OffsetDateTime.class),
                rs.getObject("atualizado_em", OffsetDateTime.class)));
    }

    @Override
    public boolean updateGestor(int codigo, GestorChanges changes, OffsetDateTime newUpdatedAt,
            OffsetDateTime expectedUpdatedAt) {
        if (changes.isEmpty()) {
            return false;
        }
        List<Object> args = new ArrayList<>();
        for (String column : changes.columns()) {
            args.add(switch (column) {
                case "nome" -> changes.nome();
                case "email" -> changes.email();
                case "senha" -> changes.senha();
                default -> changes.telefone();
            });
        }
        args.add(newUpdatedAt);
        args.add(codigo);
        if (expectedUpdatedAt != null) {
            args.add(expectedUpdatedAt);
        }
        return jdbc.update(updateGestorSql(changes, expectedUpdatedAt == null), args.toArray()) == 1;
    }

    // Colunas vem de uma lista fechada (GestorChanges.columns), nunca de entrada externa.
    static String updateGestorSql(GestorChanges changes, boolean expectedIsNull) {
        StringBuilder sql = new StringBuilder("UPDATE gestor SET ");
        for (String column : changes.columns()) {
            sql.append(column).append(" = ?, ");
        }
        sql.append("atualizado_em = ? WHERE codigo = ? AND atualizado_em ")
                .append(expectedIsNull ? "IS NULL" : "= ?");
        return sql.toString();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
