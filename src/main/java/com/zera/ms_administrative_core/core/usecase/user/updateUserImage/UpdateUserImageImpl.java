package com.zera.ms_administrative_core.core.usecase.user.updateUserImage;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserImageImpl implements UpdateUserImage {

    private final UserRepository userRepository;

    public UpdateUserImageImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(UUID userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateImageUrl(imageUrl);
        userRepository.save(user);
    }
}
