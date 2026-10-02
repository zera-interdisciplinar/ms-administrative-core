package com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;

@Service
public class FindDailyActiveUsersImpl implements FindDailyActiveUsers {

    /**
     * A view calcula media movel e acumulado com window function sobre a serie inteira; um periodo
     * aberto traria o historico todo para a memoria da aplicacao a cada request de dashboard.
     */
    private static final long JANELA_MAXIMA_DIAS = 366;

    private final AnalyticsRepository analytics;

    public FindDailyActiveUsersImpl(AnalyticsRepository analytics) {
        this.analytics = analytics;
    }

    @Override
    public List<DailyActiveUsers> execute(LocalDate de, LocalDate ate) {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        if (ChronoUnit.DAYS.between(de, ate) > JANELA_MAXIMA_DIAS) {
            throw new IllegalArgumentException("Periodo maximo de consulta: " + JANELA_MAXIMA_DIAS + " dias");
        }
        return analytics.dailyActiveUsers(de, ate);
    }
}
