package com.zera.ms_administrative_core.core.usecase.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;
import com.zera.ms_administrative_core.core.repository.MonthlyUnitAlerts;
import com.zera.ms_administrative_core.core.repository.UnitRanking;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers.FindDailyActiveUsersImpl;
import com.zera.ms_administrative_core.core.usecase.analytics.findMonthlyUnitAlerts.FindMonthlyUnitAlertsImpl;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitHealth.FindUnitHealthImpl;
import com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking.FindUnitRankingImpl;

@ExtendWith(MockitoExtension.class)
class AnalyticsUseCasesTest {

    @Mock
    private AnalyticsRepository analytics;
    @Mock
    private UnitRepository units;

    private final UUID unidadeId = UUID.randomUUID();

    private static MonthlyUnitAlerts linha(UUID unidadeId, int total, long acumulado) {
        return new MonthlyUnitAlerts(LocalDate.of(2026, 3, 1), "2026-03", unidadeId, "Unidade A",
                total, total * 3, 1, BigDecimal.ONE, acumulado, 2, BigDecimal.TEN, 5,
                BigDecimal.valueOf(12.5));
    }

    // --- serie mensal ---

    @Test
    @DisplayName("Deve devolver a serie mensal da unidade")
    void shouldReturnMonthlySeries() {
        when(units.findById(unidadeId)).thenReturn(Optional.of(mockUnit()));
        when(analytics.monthlyAlertsByUnit(unidadeId)).thenReturn(List.of(linha(unidadeId, 10, 30)));

        List<MonthlyUnitAlerts> resultado =
                new FindMonthlyUnitAlertsImpl(analytics, units).execute(unidadeId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().totalAcumulado()).isEqualTo(30);
    }

    /**
     * Unidade inexistente e 404, nao lista vazia: lista vazia afirma "essa unidade nao teve
     * alerta", o que e uma informacao diferente de "essa unidade nao existe".
     */
    @Test
    @DisplayName("Deve dar 404 para unidade inexistente em vez de lista vazia")
    void shouldFailForUnknownUnitOnMonthly() {
        when(units.findById(unidadeId)).thenReturn(Optional.empty());
        FindMonthlyUnitAlertsImpl useCase = new FindMonthlyUnitAlertsImpl(analytics, units);

        assertThatThrownBy(() -> useCase.execute(unidadeId))
                .isInstanceOf(UnitNotFoundException.class);

        verify(analytics, never()).monthlyAlertsByUnit(unidadeId);
    }

    // --- ranking ---

    @Test
    @DisplayName("Deve limitar o ranking ao teto de 200 mesmo se pedirem mais")
    void shouldCapRankingLimit() {
        when(analytics.unitRanking(200)).thenReturn(List.of());

        new FindUnitRankingImpl(analytics).execute(5000);

        verify(analytics).unitRanking(200);
    }

    @Test
    @DisplayName("Deve repassar o limite pedido quando dentro do teto")
    void shouldPassThroughRankingLimit() {
        UnitRanking ranking = new UnitRanking(unidadeId, "Unidade A", "Santos", "SP", 9, 27, 2, 5,
                BigDecimal.valueOf(5.4), 1, 1, BigDecimal.valueOf(100), BigDecimal.valueOf(40));
        when(analytics.unitRanking(10)).thenReturn(List.of(ranking));

        assertThat(new FindUnitRankingImpl(analytics).execute(10))
                .singleElement().extracting(UnitRanking::quartilGravidade).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve recusar limite zero ou negativo")
    void shouldRejectInvalidLimit() {
        FindUnitRankingImpl useCase = new FindUnitRankingImpl(analytics);

        assertThatThrownBy(() -> useCase.execute(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(-3)).isInstanceOf(IllegalArgumentException.class);
        verify(analytics, never()).unitRanking(anyInt());
    }

    // --- DAU ---

    @Test
    @DisplayName("Deve devolver a serie de DAU do periodo")
    void shouldReturnDauSeries() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = LocalDate.of(2026, 1, 7);
        DailyActiveUsers dia = new DailyActiveUsers(de, 12, 20, BigDecimal.valueOf(10.5), 100, 2,
                18, BigDecimal.valueOf(66.7));
        when(analytics.dailyActiveUsers(de, ate)).thenReturn(List.of(dia));

        assertThat(new FindDailyActiveUsersImpl(analytics).execute(de, ate))
                .singleElement().extracting(DailyActiveUsers::dau).isEqualTo(12);
    }

    /**
     * A view calcula media movel e acumulado com window function sobre a serie toda; periodo aberto
     * traria o historico inteiro para a memoria a cada request de dashboard.
     */
    @Test
    @DisplayName("Deve recusar janela maior que 366 dias")
    void shouldRejectTooWideWindow() {
        FindDailyActiveUsersImpl useCase = new FindDailyActiveUsersImpl(analytics);
        LocalDate de = LocalDate.of(2024, 1, 1);
        LocalDate ate = de.plusDays(400);

        assertThatThrownBy(() -> useCase.execute(de, ate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
    }

    @Test
    @DisplayName("Deve recusar periodo invertido ou nulo no DAU")
    void shouldRejectInvalidDauPeriod() {
        FindDailyActiveUsersImpl useCase = new FindDailyActiveUsersImpl(analytics);
        LocalDate hoje = LocalDate.of(2026, 2, 2);

        assertThatThrownBy(() -> useCase.execute(hoje, hoje.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null, hoje))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- indice de saude ---

    @Test
    @DisplayName("Deve devolver o indice de saude calculado pela function")
    void shouldReturnHealthIndex() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = LocalDate.of(2026, 3, 31);
        when(units.findById(unidadeId)).thenReturn(Optional.of(mockUnit()));
        when(analytics.unitHealthIndex(unidadeId, de, ate)).thenReturn(BigDecimal.valueOf(72.5));

        assertThat(new FindUnitHealthImpl(analytics, units).execute(unidadeId, de, ate))
                .isEqualByComparingTo("72.5");
    }

    @Test
    @DisplayName("Deve validar o periodo antes de consultar a unidade")
    void shouldValidatePeriodBeforeUnit() {
        FindUnitHealthImpl useCase = new FindUnitHealthImpl(analytics, units);
        LocalDate hoje = LocalDate.of(2026, 4, 4);

        assertThatThrownBy(() -> useCase.execute(unidadeId, hoje, hoje.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(units, never()).findById(unidadeId);
    }

    @Test
    @DisplayName("Deve dar 404 de unidade no indice de saude")
    void shouldFailForUnknownUnitOnHealth() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        when(units.findById(unidadeId)).thenReturn(Optional.empty());
        FindUnitHealthImpl useCase = new FindUnitHealthImpl(analytics, units);

        assertThatThrownBy(() -> useCase.execute(unidadeId, de, de.plusDays(10)))
                .isInstanceOf(UnitNotFoundException.class);
    }

    private Unit mockUnit() {
        return org.mockito.Mockito.mock(Unit.class);
    }
}
