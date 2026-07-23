package com.zera.ms_administrative_core.core.domain.service;

import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;

public interface PasswordHasher {
    HashedPassword hash(RawPassword rawPassword);
    boolean matches(RawPassword rawPassword, HashedPassword hashedPassword);
}
