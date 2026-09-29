package com.zera.ms_administrative_core.core.usecase.updateUserImage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.updateUserImage.UpdateUserImageImpl;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class UpdateUserImageImplTest {

    private User newUser() {
        return new Employee(
                UUID.fromString("00000000-0000-0000-0000-000000000050"),
                "Image User",
                new Email("image@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000051"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
    }

    @Test
    void executeShouldUpdateImageUrl() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = newUser();
        repository.save(user);

        UpdateUserImageImpl useCase = new UpdateUserImageImpl(repository);
        useCase.execute(user.getUserId(), "https://cdn.example.com/avatar.png");

        assertEquals("https://cdn.example.com/avatar.png",
                repository.findById(user.getUserId()).orElseThrow().getImageUrl());
    }

    @Test
    void executeShouldAllowClearingImageUrl() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = newUser();
        user.updateImageUrl("https://cdn.example.com/old.png");
        repository.save(user);

        UpdateUserImageImpl useCase = new UpdateUserImageImpl(repository);
        useCase.execute(user.getUserId(), null);

        assertNull(repository.findById(user.getUserId()).orElseThrow().getImageUrl());
    }

    @Test
    void executeShouldRejectWhenUserDoesNotExist() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        UpdateUserImageImpl useCase = new UpdateUserImageImpl(repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.fromString("00000000-0000-0000-0000-000000000052"),
                        "https://cdn.example.com/avatar.png"));
        assertEquals("User not found", exception.getMessage());
    }
}
