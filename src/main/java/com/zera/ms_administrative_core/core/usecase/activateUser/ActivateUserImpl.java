package com.zera.ms_administrative_core.core.usecase.activateUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivateUserImpl implements ActivateUser {

    private final UserRepository repository;

    public ActivateUserImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.activate();
        repository.save(user);
    }
}
