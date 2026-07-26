package com.zera.ms_administrative_core.core.usecase.user.suspendUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
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
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.suspend();
        repository.save(user);
    }
}
