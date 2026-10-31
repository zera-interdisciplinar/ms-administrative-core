package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

public interface ConfirmPasswordReset {
    void execute(String rawToken, String newRawPassword);
}
