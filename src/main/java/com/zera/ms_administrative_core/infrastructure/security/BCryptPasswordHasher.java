package com.zera.ms_administrative_core.infrastructure.security;

import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public HashedPassword hash(RawPassword rawPassword) {
        return new HashedPassword(encoder.encode(rawPassword.value()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, HashedPassword hashedPassword) {
        return encoder.matches(rawPassword.value(), hashedPassword.hash());
    }
}