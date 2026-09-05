package com.zera.ms_administrative_core.core.usecase.auth;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class AuthenticateUserImpl implements AuthenticateUser {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateUserImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public AuthenticatedUser execute(String email, String rawPassword) {
        Email parsedEmail;
        try {
            parsedEmail = new Email(email);
        } catch (RuntimeException malformed) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(parsedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.getStatus().isActive()) {
            throw new InvalidCredentialsException();
        }

        if (rawPassword == null
                || !passwordHasher.matches(new RawPassword(rawPassword), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new AuthenticatedUser(user.getUserId(), user.getEmail().value(), user.role());
    }
}
