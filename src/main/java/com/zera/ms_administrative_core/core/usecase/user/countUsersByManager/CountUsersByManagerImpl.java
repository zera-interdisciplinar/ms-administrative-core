package com.zera.ms_administrative_core.core.usecase.user.countUsersByManager;

import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CountUsersByManagerImpl implements CountUsersByManager {

    private final UserRepository repository;

    public CountUsersByManagerImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ManagerUserCountOutput> execute() {
        return repository.countEmployeesGroupedByManager().stream()
                .map(ManagerUserCountOutput::from)
                .toList();
    }
}
