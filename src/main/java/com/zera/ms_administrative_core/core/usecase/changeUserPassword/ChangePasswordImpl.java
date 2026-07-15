package com.zera.ms_administrative_core.core.usecase.changeUserPassword;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.repository.UserRepository;

public class ChangePasswordImpl implements ChangePassword {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public ChangePasswordImpl(UserRepository repository,
            PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void execute(ChangePasswordCommand command) {
        if (!command.rawPassword().equals(command.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = repository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        HashedPassword hashedPassword = passwordHasher.hash(new RawPassword(command.rawPassword()));
        user.changePassword(hashedPassword);
        repository.save(user);
    }
}
