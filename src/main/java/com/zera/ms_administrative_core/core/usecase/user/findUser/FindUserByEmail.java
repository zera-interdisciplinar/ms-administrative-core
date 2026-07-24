package com.zera.ms_administrative_core.core.usecase.user.findUser;

public interface FindUserByEmail {
    UserOutput execute(String email);
}