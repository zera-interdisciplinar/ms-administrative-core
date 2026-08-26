package com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode;

import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;

public interface RegisterWithInvitationCode {
    RegisterUserOutput execute(RegisterWithInvitationCodeCommand command);
}
