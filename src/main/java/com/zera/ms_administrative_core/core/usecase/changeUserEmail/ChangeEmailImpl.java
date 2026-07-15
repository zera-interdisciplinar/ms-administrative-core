package com.zera.ms_administrative_core.core.usecase.changeUserEmail;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.UserRepository;

public class ChangeEmailImpl implements ChangeEmail {

    private final UserRepository userRepository;

    public ChangeEmailImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(UUID userId, String newEmail) {
        Email email = new Email(newEmail);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.changeEmail(email);
        userRepository.save(user);
    }
}
