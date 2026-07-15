package com.zera.ms_administrative_core.core.usecase.renameUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class RenameUserImplTest {

    @Test
    void executeShouldRenameUser() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new Employee(
                UUID.fromString("00000000-0000-0000-0000-000000000040"),
                "Old Name",
                new Email("rename@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000041"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        repository.save(user);

        RenameUserImpl useCase = new RenameUserImpl(repository);
        useCase.execute(user.getUserId(), "New Name");

        assertEquals("New Name", repository.findById(user.getUserId()).orElseThrow().getName());
    }

    @Test
    void executeShouldRejectWhenUserDoesNotExist() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        RenameUserImpl useCase = new RenameUserImpl(repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.fromString("00000000-0000-0000-0000-000000000042"), "New Name"));
        assertEquals("Usuário não encontrado", exception.getMessage());
    }
}
