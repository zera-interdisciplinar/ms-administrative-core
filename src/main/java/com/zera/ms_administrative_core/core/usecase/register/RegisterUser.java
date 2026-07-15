package com.zera.ms_administrative_core.core.usecase.register;

public interface RegisterUser {
    RegisterUserOutput execute(RegisterUserCommand command);
}