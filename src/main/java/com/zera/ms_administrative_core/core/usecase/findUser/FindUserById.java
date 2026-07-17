package com.zera.ms_administrative_core.core.usecase.findUser;

import java.util.UUID;

public interface FindUserById {
    UserOutput execute(UUID userId);
}
