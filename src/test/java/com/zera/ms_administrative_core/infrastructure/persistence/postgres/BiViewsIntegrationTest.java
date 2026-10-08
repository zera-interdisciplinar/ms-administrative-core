package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRanking;

/**
 * Prova as views dimensionais da V15 com VALORES ESPERADOS, nao apenas "a consulta roda".
 *
 * <p>Window function sem conferencia de valor e decoracao: um {@code SUM() OVER} com a janela errada
 * devolve numero plausivel e errado, e uma assercao de "nao vazio" passaria feliz.
 */
@Transactional
class BiViewsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AnalyticsRepository analytics;

    // ------------------------------------------------------------------ dim_tempo

    @Test
    @DisplayName("dim_tempo deve conter todos os dias do periodo, inclusive os sem fato")
    void shouldGenerateCompleteCalendar() {
        Integer dias = jdbc.queryForObject("""
                SELECT count(*) FROM bi.dim_tempo
                 WHERE data_id BETWEEN DATE '2025-01-01' AND DATE '2025-12-31'
                """, Integer.class);

        assertThat(dias).isEqualTo(365);
    }

    /**
     * REGRESSAO. Com o inicio do calendario fixo em 2024-01-01, todo alerta anterior sumia da view
     * mensal: o JOIN com a dimensao descartava o fato em silencio -- sem erro, so um total menor.
     * O fato mostrava 2 alertas e a view mensal, 1. E o pior tipo de bug de BI, porque o numero
     * errado parece plausivel; e so um teste que compara FATO com VIEW o pega.
     */
    @Test
    @DisplayName("Fato antigo nao pode sumir da view mensal por estar fora do calendario")
    void shouldNotDropFactsOlderThanCalendarStart() {
        Fixture f = novaHierarquia();
        inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.of(2023, 6, 15, 10, 0));
        inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.now().minusDays(5).withNano(0));

        int noFato = jdbc.queryForObject(
                "SELECT count(*) FROM bi.fato_alerta WHERE unidade_id = ?", Integer.class,
                f.unidadeId());
        int naView = analytics.monthlyAlertsByUnit(f.unidadeId()).stream()
                .mapToInt(MonthlyUnitAlerts::totalAlertas).sum();

        assertThat(naView)
                .as("a view mensal precisa somar o mesmo que o fato; diferenca = fato descartado pelo JOIN")
                .isEqualTo(noFato);
    }

    @Test
    @DisplayName("Calendario deve comecar antes do fato mais antigo")
    void calendarShouldCoverOldestFact() {
        Fixture f = novaHierarquia();
        inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "OPEN",
                LocalDateTime.of(2023, 2, 1, 8, 0));

        LocalDate inicioCalendario = jdbc.queryForObject(
                "SELECT min(data_id) FROM bi.dim_tempo", LocalDate.class);
        LocalDate fatoMaisAntigo = jdbc.queryForObject(
                "SELECT min(data_id) FROM bi.fato_alerta", LocalDate.class);

        assertThat(inicioCalendario).isBeforeOrEqualTo(fatoMaisAntigo);
    }

    @Test
    @DisplayName("dim_tempo deve classificar fim de semana corretamente")
    void shouldFlagWeekend() {
        Map<String, Object> sabado = jdbc.queryForMap(
                "SELECT dia_da_semana, fim_de_semana FROM bi.dim_tempo WHERE data_id = DATE '2025-03-01'");

        assertThat(sabado.get("dia_da_semana")).isEqualTo(6);
        assertThat(sabado.get("fim_de_semana")).isEqualTo(true);
    }

    // -------------------------------------------------------------- dim_unidade

    /**
     * `address.unit_id` nao tem unicidade no schema: uma unidade PODE ter dois enderecos. Sem o
     * DISTINCT ON, a dimensao duplicaria a unidade e TODO fato ligado a ela seria contado em dobro.
     * Este e o teste do fan-out de join.
     */
    @Test
    @DisplayName("dim_unidade nao deve duplicar unidade com mais de um endereco")
    void shouldNotDuplicateUnitWithTwoAddresses() {
        Fixture f = novaHierarquia();
        inserirEndereco(f.unidadeId(), "Santos", LocalDateTime.now().minusDays(10));
        inserirEndereco(f.unidadeId(), "Campinas", LocalDateTime.now());

        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT unidade_id, cidade FROM bi.dim_unidade WHERE unidade_id = ?", f.unidadeId());

        assertThat(linhas).hasSize(1);
        // e fica com o endereco MAIS RECENTE
        assertThat(linhas.getFirst().get("cidade")).isEqualTo("Campinas");
    }

    // --------------------------------------------------------------- fato_alerta

    @Test
    @DisplayName("fato_alerta deve pesar severidade em 1/3/9")
    void shouldWeighSeverity() {
        Fixture f = novaHierarquia();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1).withNano(0);
        UUID baixo = inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "OPEN", ontem);
        UUID medio = inserirAlerta(f.unidadeId(), f.gestorId(), "MEDIUM", "OPEN", ontem);
        UUID alto = inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN", ontem);

        assertThat(peso(baixo)).isEqualTo(1);
        assertThat(peso(medio)).isEqualTo(3);
        assertThat(peso(alto)).isEqualTo(9);
    }

    @Test
    @DisplayName("fato_alerta deve calcular horas ate fechamento apenas para alerta fechado")
    void shouldComputeHoursToClose() {
        Fixture f = novaHierarquia();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1).withNano(0);
        UUID aberto = inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "OPEN", ontem);
        UUID fechado = inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "CLOSED", ontem);

        assertThat(jdbc.queryForMap(
                "SELECT horas_ate_fechamento FROM bi.fato_alerta WHERE alerta_id = ?", aberto)
                .get("horas_ate_fechamento")).isNull();
        // os fixtures fecham 2h depois do evento
        assertThat(jdbc.queryForObject(
                "SELECT horas_ate_fechamento FROM bi.fato_alerta WHERE alerta_id = ?",
                java.math.BigDecimal.class, fechado)).isEqualByComparingTo("2.00");
    }

    // ------------------------------------------- vw_alertas_mensal_unidade

    /**
     * Tres meses com 1, 2 e 3 alertas devem produzir acumulado 1, 3, 6 e LAG 1, 2. Se a janela
     * estivesse sem ORDER BY ou com PARTITION errado, os numeros seriam outros.
     */
    @Test
    @DisplayName("Running total, LAG e variacao MoM devem bater com os valores esperados")
    void shouldComputeRunningTotalAndLag() {
        Fixture f = novaHierarquia();
        LocalDate base = LocalDate.now().withDayOfMonth(1).minusMonths(3);
        criarAlertasNoMes(f, base, 1);
        criarAlertasNoMes(f, base.plusMonths(1), 2);
        criarAlertasNoMes(f, base.plusMonths(2), 3);

        List<MonthlyUnitAlerts> serie = analytics.monthlyAlertsByUnit(f.unidadeId());

        assertThat(serie).hasSize(3);
        assertThat(serie).extracting(MonthlyUnitAlerts::totalAlertas).containsExactly(1, 2, 3);
        assertThat(serie).extracting(MonthlyUnitAlerts::totalAcumulado).containsExactly(1L, 3L, 6L);
        assertThat(serie.get(0).variacaoMom()).isEqualTo(1);   // sem mes anterior: COALESCE(0)
        assertThat(serie.get(1).variacaoMom()).isEqualTo(1);   // 2 - 1
        assertThat(serie.get(2).variacaoMom()).isEqualTo(1);   // 3 - 2
        assertThat(serie.get(1).variacaoMomPercentual()).isEqualByComparingTo("100.00");
        assertThat(serie.get(2).variacaoMomPercentual()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Media movel de 3 meses deve considerar a janela de 3 linhas")
    void shouldComputeThreeMonthMovingAverage() {
        Fixture f = novaHierarquia();
        LocalDate base = LocalDate.now().withDayOfMonth(1).minusMonths(3);
        criarAlertasNoMes(f, base, 3);
        criarAlertasNoMes(f, base.plusMonths(1), 6);
        criarAlertasNoMes(f, base.plusMonths(2), 9);

        List<MonthlyUnitAlerts> serie = analytics.monthlyAlertsByUnit(f.unidadeId());

        assertThat(serie.get(0).mediaMovel3m()).isEqualByComparingTo("3.00");
        assertThat(serie.get(1).mediaMovel3m()).isEqualByComparingTo("4.50");
        assertThat(serie.get(2).mediaMovel3m()).isEqualByComparingTo("6.00");
    }

    // ------------------------------------------------- vw_ranking_unidades

    @Test
    @DisplayName("Ranking deve ordenar por peso e atribuir quartis")
    void shouldRankUnitsByWeight() {
        Fixture pior = novaHierarquia();
        Fixture melhor = novaHierarquia();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1).withNano(0);
        for (int i = 0; i < 5; i++) {
            inserirAlerta(pior.unidadeId(), pior.gestorId(), "HIGH", "OPEN", ontem);
        }
        inserirAlerta(melhor.unidadeId(), melhor.gestorId(), "LOW", "OPEN", ontem);

        List<UnitRanking> ranking = analytics.unitRanking(200);
        UnitRanking linhaPior = acha(ranking, pior.unidadeId());
        UnitRanking linhaMelhor = acha(ranking, melhor.unidadeId());

        assertThat(linhaPior.pesoTotal()).isEqualTo(45);
        assertThat(linhaMelhor.pesoTotal()).isEqualTo(1);
        assertThat(linhaPior.rankingGeral()).isLessThan(linhaMelhor.rankingGeral());
        assertThat(linhaPior.quartilGravidade()).isBetween(1, 4);
        assertThat(linhaPior.usuariosAtivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unidade sem alerta deve aparecer no ranking com zero, nao desaparecer")
    void shouldKeepUnitsWithoutAlerts() {
        Fixture semAlerta = novaHierarquia();

        UnitRanking linha = acha(analytics.unitRanking(200), semAlerta.unidadeId());

        assertThat(linha.totalAlertas()).isZero();
        assertThat(linha.pesoTotal()).isZero();
    }

    // ------------------------------------------------------ vw_dau_diario

    @Test
    @DisplayName("Dia sem acesso deve aparecer com DAU zero, nao ser omitido")
    void shouldShowZeroDaysInDauSeries() {
        LocalDate ate = LocalDate.now();
        LocalDate de = ate.minusDays(6);

        var serie = analytics.dailyActiveUsers(de, ate);

        assertThat(serie).hasSize(7);
        assertThat(serie).allSatisfy(d -> assertThat(d.dau()).isGreaterThanOrEqualTo(0));
    }

    // ------------------------------------------- vw_hierarquia_equipe

    @Test
    @DisplayName("Hierarquia recursiva deve calcular nivel e caminho")
    void shouldBuildHierarchyPath() {
        Fixture f = novaHierarquia();
        UUID coordenador = UUID.randomUUID();
        UUID funcionario = UUID.randomUUID();
        inserirUsuario(coordenador, f.unidadeId(), f.gestorId(), "MANAGER", "ACTIVE");
        inserirUsuario(funcionario, f.unidadeId(), coordenador, "EMPLOYEE", "ACTIVE");

        Map<String, Object> folha = jdbc.queryForMap("""
                SELECT nivel, caminho_texto, raiz_id, tamanho_equipe
                  FROM bi.vw_hierarquia_equipe WHERE usuario_id = ?
                """, funcionario);

        assertThat(folha.get("nivel")).isEqualTo(3);
        assertThat((String) folha.get("caminho_texto")).contains(" > ");
        assertThat(folha.get("raiz_id")).isEqualTo(f.gestorId());
        assertThat(folha.get("tamanho_equipe")).isEqualTo(0);
    }

    // ------------------------------------------------------------ helpers

    private void criarAlertasNoMes(Fixture f, LocalDate mes, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            inserirAlerta(f.unidadeId(), f.gestorId(), "LOW", "OPEN", mes.atTime(10, 0));
        }
    }

    private void inserirEndereco(UUID unidadeId, String cidade, LocalDateTime criadoEm) {
        jdbc.update("""
                INSERT INTO address (id, city, state, cep, number, unit_id, created_at, updated_at)
                VALUES (?, ?, 'SP', '01310100', '100', ?, ?, ?)
                """, UUID.randomUUID(), cidade, unidadeId, criadoEm, criadoEm);
    }

    private int peso(UUID alertaId) {
        return jdbc.queryForObject(
                "SELECT peso_severidade FROM bi.fato_alerta WHERE alerta_id = ?", Integer.class, alertaId);
    }

    private UnitRanking acha(List<UnitRanking> ranking, UUID unidadeId) {
        return ranking.stream().filter(r -> r.unidadeId().equals(unidadeId)).findFirst()
                .orElseThrow(() -> new AssertionError("unidade ausente do ranking: " + unidadeId));
    }
}
