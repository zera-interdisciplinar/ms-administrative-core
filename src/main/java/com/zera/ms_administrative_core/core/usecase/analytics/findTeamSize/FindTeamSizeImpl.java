package com.zera.ms_administrative_core.core.usecase.analytics.findTeamSize;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class FindTeamSizeImpl implements FindTeamSize {

    private final AnalyticsRepository analytics;
    private final UserRepository users;

    public FindTeamSizeImpl(AnalyticsRepository analytics, UserRepository users) {
        this.analytics = analytics;
        this.users = users;
    }

    @Override
    public int execute(UUID gestorId) {
        // 404 e nao zero: zero diria "esse gestor existe e nao tem equipe", o que e outra afirmacao.
        if (users.findById(gestorId).isEmpty()) {
            throw new UserNotFoundException(gestorId);
        }
        return analytics.teamSize(gestorId);
    }
}
