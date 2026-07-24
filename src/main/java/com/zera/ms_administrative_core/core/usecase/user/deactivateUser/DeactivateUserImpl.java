package com.zera.ms_administrative_core.core.usecase.user.deactivateUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DeactivateUserImpl implements DeactivateUser {

    private final UserRepository repository;

    public DeactivateUserImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.deactivate();
        repository.save(user);
    }
}
