package com.zera.ms_administrative_core.core.usecase.suspendUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class SuspendUserImpl implements SuspendUser {

    private final UserRepository repository;

    public SuspendUserImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.suspend();
        repository.save(user);
    }
}
