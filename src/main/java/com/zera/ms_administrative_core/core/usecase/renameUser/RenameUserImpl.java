package com.zera.ms_administrative_core.core.usecase.renameUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.repository.UserRepository;

    public class RenameUserImpl implements RenameUser {

    private final UserRepository userRepository;

    public RenameUserImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(UUID userId, String newName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.rename(newName);
        userRepository.save(user);
    }
}
