package com.zera.ms_administrative_core.core.usecase.analytics.findDailyActiveUsers;

import java.time.LocalDate;
import java.util.List;

import com.zera.ms_administrative_core.core.repository.DailyActiveUsers;

public interface FindDailyActiveUsers {
    List<DailyActiveUsers> execute(LocalDate de, LocalDate ate);
}
