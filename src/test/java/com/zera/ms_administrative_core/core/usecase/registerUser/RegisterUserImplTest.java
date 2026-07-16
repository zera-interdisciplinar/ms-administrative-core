package com.zera.ms_administrative_core.core.usecase.registerUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class RegisterUserImplTest {

    @Test
    void executeShouldCreateAndPersistUserWhenEmailIsFree() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        RegisterUserImpl useCase = new RegisterUserImpl(repository, new FixedPasswordHasher());
        RegisterUserCommand command = new RegisterUserCommand(
                "Ana",
                Role.MANAGER,
                "plain-password",
                "ana@example.com",
                UUID.fromString("00000000-0000-0000-0000-000000000020"));

        RegisterUserOutput output = useCase.execute(command);

        assertEquals("Ana", output.name());
        assertEquals(new Email("ana@example.com"), output.email());
        assertEquals(Role.MANAGER, output.role());
        assertTrue(repository.findById(output.userId()).isPresent());

        User savedUser = repository.findById(output.userId()).orElseThrow();
        assertEquals(Status.ACTIVE, savedUser.getStatus());
        assertEquals(new HashedPassword("hashed:plain-password"), savedUser.getPassword());
    }

    @Test
    void executeShouldRejectDuplicateEmail() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        RegisterUserImpl useCase = new RegisterUserImpl(repository, new FixedPasswordHasher());
        RegisterUserCommand command = new RegisterUserCommand(
                "Ana",
                Role.EMPLOYEE,
                "plain-password",
                "ana@example.com",
                UUID.fromString("00000000-0000-0000-0000-000000000021"));

        useCase.execute(command);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(command));
        assertEquals("User with this email already exists", exception.getMessage());
    }
}
