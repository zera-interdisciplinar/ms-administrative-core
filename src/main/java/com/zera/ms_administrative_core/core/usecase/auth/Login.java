package com.zera.ms_administrative_core.core.usecase.auth;

public interface Login {
    TokenPair execute(String email, String rawPassword);
}
