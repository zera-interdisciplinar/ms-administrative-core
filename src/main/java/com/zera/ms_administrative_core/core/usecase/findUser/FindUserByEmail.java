package com.zera.ms_administrative_core.core.usecase.findUser;

public interface FindUserByEmail {
    UserOutput execute(String email);
}