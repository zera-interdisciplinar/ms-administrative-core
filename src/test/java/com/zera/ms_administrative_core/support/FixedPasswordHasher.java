package com.zera.ms_administrative_core.support;

import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;

public class FixedPasswordHasher implements PasswordHasher {

    @Override
    public HashedPassword hash(RawPassword rawPassword) {
        return new HashedPassword("hashed:" + rawPassword.value());
    }

    @Override
    public boolean matches(RawPassword rawPassword, HashedPassword hashedPassword) {
        return hashedPassword.hash().equals("hashed:" + rawPassword.value());
    }
}
