package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRanking;

/**
 * Le a camada analitica por SQL direto.
 *
 * <p>As views de {@code bi} usam window function, CTE recursiva e LATERAL -- construcoes que o JPQL
 * nao expressa e que o H2 dos testes unitarios nem suporta. Mapear as views como {@code @Entity}
 * faria o Hibernate cria-las como TABELA VAZIA no H2 e os testes passariam sem nunca executar uma
 * linha do SQL analitico real. Quem cobre esta classe e teste com Postgres de verdade.
 */
@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private static final RowMapper<MonthlyUnitAlerts> MENSAL = (rs, i) -> new MonthlyUnitAlerts(
            rs.getObject("mes", LocalDate.class),
            rs.getString("ano_mes"),
            rs.getObject("unidade_id", UUID.class),
            rs.getString("unidade"),
            rs.getInt("total_alertas"),
            rs.getInt("peso_total"),
            rs.getInt("alertas_abertos"),
            rs.getBigDecimal("media_horas_fechamento"),
            rs.getLong("total_acumulado"),
            rs.getInt("ranking_no_mes"),
            rs.getBigDecimal("media_movel_3m"),
            rs.getInt("variacao_mom"),
            rs.getBigDecimal("variacao_mom_percentual"));

    private static final RowMapper<UnitRanking> RANKING = (rs, i) -> new UnitRanking(
            rs.getObject("unidade_id", UUID.class),
            rs.getString("unidade"),
            rs.getString("cidade"),
            rs.getString("estado"),
            rs.getInt("total_alertas"),
            rs.getInt("peso_total"),
            rs.getInt("alertas_abertos"),
            rs.getInt("usuarios_ativos"),
            rs.getBigDecimal("peso_por_usuario"),
            rs.getInt("ranking_geral"),
            rs.getInt("quartil_gravidade"),
            rs.getBigDecimal("percentil"),
            rs.getBigDecimal("participacao_percentual"));

    private static final RowMapper<DailyActiveUsers> DAU = (rs, i) -> new DailyActiveUsers(
            rs.getObject("data_id", LocalDate.class),
            rs.getInt("dau"),
            rs.getInt("acessos"),
            rs.getBigDecimal("dau_media_movel_7d"),
            rs.getLong("acessos_acumulados"),
            rs.getInt("variacao_dod"),
            rs.getInt("usuarios_unicos_7d"),
            rs.getBigDecimal("aderencia_dau_wau"));

    private final JdbcTemplate jdbc;

    public AnalyticsRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MonthlyUnitAlerts> monthlyAlertsByUnit(UUID unidadeId) {
        return jdbc.query("""
                SELECT mes, ano_mes, unidade_id, unidade, total_alertas, peso_total, alertas_abertos,
                       media_horas_fechamento, total_acumulado, ranking_no_mes, media_movel_3m,
                       variacao_mom, variacao_mom_percentual
                  FROM bi.vw_alertas_mensal_unidade
                 WHERE unidade_id = ?
                 ORDER BY mes
                """, MENSAL, unidadeId);
    }

    @Override
    public List<UnitRanking> unitRanking(int limite) {
        return jdbc.query("""
                SELECT unidade_id, unidade, cidade, estado, total_alertas, peso_total,
                       alertas_abertos, usuarios_ativos, peso_por_usuario, ranking_geral,
                       quartil_gravidade, percentil, participacao_percentual
                  FROM bi.vw_ranking_unidades
                 ORDER BY ranking_geral
                 LIMIT ?
                """, RANKING, limite);
    }

    @Override
    public List<DailyActiveUsers> dailyActiveUsers(LocalDate de, LocalDate ate) {
        return jdbc.query("""
                SELECT data_id, dau, acessos, dau_media_movel_7d, acessos_acumulados,
                       variacao_dod, usuarios_unicos_7d, aderencia_dau_wau
                  FROM bi.vw_dau_diario
                 WHERE data_id BETWEEN ? AND ?
                 ORDER BY data_id
                """, DAU, de, ate);
    }

    @Override
    public BigDecimal unitHealthIndex(UUID unidadeId, LocalDate de, LocalDate ate) {
        return jdbc.queryForObject(
                "SELECT fn_indice_saude_unidade(?, ?, ?)", BigDecimal.class, unidadeId, de, ate);
    }
}
