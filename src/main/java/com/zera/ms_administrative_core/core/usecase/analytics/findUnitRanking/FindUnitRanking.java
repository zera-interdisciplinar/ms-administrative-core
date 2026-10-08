package com.zera.ms_administrative_core.core.usecase.analytics.findUnitRanking;

import java.util.List;

import com.zera.ms_administrative_core.core.repository.UnitRanking;

public interface FindUnitRanking {
    List<UnitRanking> execute(int limite);
}
