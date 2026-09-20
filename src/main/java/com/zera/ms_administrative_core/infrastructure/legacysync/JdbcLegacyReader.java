package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

public class JdbcLegacyReader implements LegacyReader, AutoCloseable {

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

    public JdbcLegacyReader(HikariDataSource dataSource) {
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
    public void close() {
        dataSource.close();
    }
}
