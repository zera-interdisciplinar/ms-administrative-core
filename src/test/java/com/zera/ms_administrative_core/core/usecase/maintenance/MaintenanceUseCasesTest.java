package com.zera.ms_administrative_core.core.usecase.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository;
import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlertsImpl;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDauImpl;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokensImpl;

@ExtendWith(MockitoExtension.class)
class MaintenanceUseCasesTest {

    @Mock
    private MaintenanceRepository repository;

    // --- fechamento de alertas obsoletos ---

    @Test
    @DisplayName("Deve acionar a procedure e devolver quantos alertas foram fechados")
    void shouldCloseStaleAlerts() {
        when(repository.closeStaleAlerts(30)).thenReturn(12);

        assertThat(new CloseStaleAlertsImpl(repository).execute(30)).isEqualTo(12);
        verify(repository).closeStaleAlerts(30);
    }

    /**
     * O piso de 7 dias e a protecao que importa: um `dias` pequeno vindo por engano fecharia em
     * massa alertas recentes, e alerta fechado desaparece da tela do gestor.
     */
    @Test
    @DisplayName("Deve recusar janela menor que 7 dias sem tocar no banco")
    void shouldRejectTooShortWindow() {
        CloseStaleAlertsImpl useCase = new CloseStaleAlertsImpl(repository);

        assertThatThrownBy(() -> useCase.execute(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 dias");

        verify(repository, never()).closeStaleAlerts(anyInt());
    }

    // --- revogacao de tokens ---

    @Test
    @DisplayName("Deve devolver os dois contadores da procedure de tokens")
    void shouldRevokeExpiredTokens() {
        when(repository.revokeExpiredTokens(7)).thenReturn(new TokenCleanup(4, 2));

        TokenCleanup resultado = new RevokeExpiredTokensImpl(repository).execute(7);

        assertThat(resultado.revogados()).isEqualTo(4);
        assertThat(resultado.removidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve aceitar retencao zero (expurgo imediato) e recusar negativa")
    void shouldValidateRetention() {
        RevokeExpiredTokensImpl useCase = new RevokeExpiredTokensImpl(repository);
        when(repository.revokeExpiredTokens(0)).thenReturn(new TokenCleanup(1, 1));

        assertThat(useCase.execute(0).removidos()).isEqualTo(1);
        assertThatThrownBy(() -> useCase.execute(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- consolidacao de DAU ---

    @Test
    @DisplayName("Deve consolidar o DAU no periodo informado")
    void shouldConsolidateDau() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = LocalDate.of(2026, 1, 31);
        when(repository.consolidateDau(de, ate)).thenReturn(31);

        assertThat(new ConsolidateDauImpl(repository).execute(de, ate)).isEqualTo(31);
    }

    @Test
    @DisplayName("Deve recusar periodo invertido, nulo, e nao chamar o banco")
    void shouldRejectInvalidPeriod() {
        ConsolidateDauImpl useCase = new ConsolidateDauImpl(repository);
        LocalDate hoje = LocalDate.of(2026, 5, 10);

        assertThatThrownBy(() -> useCase.execute(hoje, hoje.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(null, hoje))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(hoje, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }
}
