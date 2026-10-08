package com.zera.ms_administrative_core.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.zera.ms_administrative_core.core.repository.MaintenanceRepository.TokenCleanup;
import com.zera.ms_administrative_core.core.usecase.maintenance.closeStaleAlerts.CloseStaleAlerts;
import com.zera.ms_administrative_core.core.usecase.maintenance.consolidateDau.ConsolidateDau;
import com.zera.ms_administrative_core.core.usecase.maintenance.revokeExpiredTokens.RevokeExpiredTokens;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceJobTest {

    @Mock
    private CloseStaleAlerts closeStaleAlerts;
    @Mock
    private RevokeExpiredTokens revokeExpiredTokens;
    @Mock
    private ConsolidateDau consolidateDau;

    private MaintenanceJob job(boolean fecharAlertas) {
        MaintenanceProperties props = new MaintenanceProperties(fecharAlertas, 30, 7, 7);
        return new MaintenanceJob(closeStaleAlerts, revokeExpiredTokens, consolidateDau, props);
    }

    @Test
    @DisplayName("Deve rodar higiene de token e DAU, e NAO fechar alertas por padrao")
    void shouldRunHousekeepingButNotCloseAlerts() {
        when(revokeExpiredTokens.execute(7)).thenReturn(new TokenCleanup(1, 0));
        when(consolidateDau.execute(any(), any())).thenReturn(7);

        job(false).run();

        verify(revokeExpiredTokens).execute(7);
        verify(consolidateDau).execute(any(LocalDate.class), any(LocalDate.class));
        verify(closeStaleAlerts, never()).execute(anyInt());
    }

    @Test
    @DisplayName("Deve fechar alertas somente quando habilitado explicitamente")
    void shouldCloseAlertsWhenEnabled() {
        when(revokeExpiredTokens.execute(7)).thenReturn(new TokenCleanup(0, 0));
        when(consolidateDau.execute(any(), any())).thenReturn(0);
        when(closeStaleAlerts.execute(30)).thenReturn(3);

        job(true).run();

        verify(closeStaleAlerts).execute(30);
    }

    /**
     * Uma falha nao pode cancelar o resto do ciclo: sem isolamento, a primeira excecao esconde que
     * o DAU parou de atualizar, e o sintoma aparece dias depois longe da causa.
     */
    @Test
    @DisplayName("Falha em uma tarefa nao deve impedir as outras")
    void shouldIsolateFailures() {
        when(revokeExpiredTokens.execute(7)).thenThrow(new IllegalStateException("banco fora"));
        when(consolidateDau.execute(any(), any())).thenReturn(7);
        when(closeStaleAlerts.execute(30)).thenReturn(1);

        job(true).run();

        verify(consolidateDau).execute(any(LocalDate.class), any(LocalDate.class));
        verify(closeStaleAlerts).execute(30);
    }

    @Test
    @DisplayName("Defaults das propriedades devem corrigir valores invalidos")
    void shouldNormalizeInvalidProperties() {
        MaintenanceProperties props = new MaintenanceProperties(true, 1, -5, 0);

        assertThat(props.closeStaleAlertsDays()).isEqualTo(30);
        assertThat(props.tokenRetentionDays()).isEqualTo(7);
        assertThat(props.dauConsolidationWindowDays()).isEqualTo(7);
    }
}
