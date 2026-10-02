package com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.UnitRanking;

@Service
public class FindUnitRankingImpl implements FindUnitRanking {

    private static final int LIMITE_MAXIMO = 200;

    private final AnalyticsRepository analytics;

    public FindUnitRankingImpl(AnalyticsRepository analytics) {
        this.analytics = analytics;
    }

    @Override
    public List<UnitRanking> execute(int limite) {
        if (limite < 1) {
            throw new IllegalArgumentException("Limite deve ser maior que zero");
        }
        return analytics.unitRanking(Math.min(limite, LIMITE_MAXIMO));
    }
}
