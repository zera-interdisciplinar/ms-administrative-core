package com.zera.ms_administrative_core.core.usecase.registerUser;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.UserFactory;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserImpl implements RegisterUser {

    private final UserRepository userRepository;
    private final PasswordHasher hasher;

    public RegisterUserImpl(UserRepository userRepository, PasswordHasher hasher) {
        this.userRepository = userRepository;
        this.hasher = hasher;
    }

    @Override
    public RegisterUserOutput execute(RegisterUserCommand command) {
        Email email = new Email(command.email());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        HashedPassword hashedPassword = hasher.hash(new RawPassword(command.rawPassword()));

        User user = UserFactory.create(
                command.role(),
                UUID.randomUUID(),
                command.name(),
                email,
                hashedPassword,
                Status.ACTIVE,
                command.unitId()
        );

        userRepository.save(user);
        return new RegisterUserOutput(
                user.getUserId(), user.getName(), user.getEmail(), user.role()
        );
    }
}
