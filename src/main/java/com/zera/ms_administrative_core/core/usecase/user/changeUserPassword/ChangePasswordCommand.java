package com.zera.ms_administrative_core.core.usecase.user.changeUserPassword;

import java.util.UUID;

public record ChangePasswordCommand(
    UUID userId,
    String rawPassword,
    String confirmPassword
) {}