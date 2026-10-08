package com.zera.ms_administrative_core.core.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Porta para a camada analitica (schema {@code bi}).
 *
 * <p>Nao ha entidade JPA para nada daqui, de proposito: sao views com window function e CTE
 * recursiva, que o H2 dos testes unitarios nao suporta. Mapear como {@code @Entity} faria o
 * Hibernate cria-las como TABELA no H2, e os testes passariam sem tocar em nenhuma das views reais.
 */
public interface AnalyticsRepository {

    List<MonthlyUnitAlerts> monthlyAlertsByUnit(UUID unidadeId);

    List<UnitRanking> unitRanking(int limite);

    List<DailyActiveUsers> dailyActiveUsers(LocalDate de, LocalDate ate);

    /** Indice 0-100 de saude da unidade no periodo (function {@code fn_indice_saude_unidade}). */
    BigDecimal unitHealthIndex(UUID unidadeId, LocalDate de, LocalDate ate);

    /**
     * Quantas pessoas estao abaixo deste gestor, em TODOS os niveis (function
     * {@code fn_tamanho_equipe}, CTE recursiva). Diferente de contar subordinados diretos, que
     * ja existe em CountUsersByManager: aqui um coordenador conta a equipe dele inteira.
     */
    int teamSize(UUID gestorId);
}
