package com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode;

public record RegisterWithInvitationCodeCommand(
    String code,
    String name,
    String rawPassword,
    String email
) {}
