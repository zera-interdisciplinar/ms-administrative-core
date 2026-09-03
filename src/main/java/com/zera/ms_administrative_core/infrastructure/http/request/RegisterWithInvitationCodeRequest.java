package com.zera.ms_administrative_core.infrastructure.http.request;

import com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode.RegisterWithInvitationCodeCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterWithInvitationCodeRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must contain exactly 6 digits") String code,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String rawPassword
) {
    public RegisterWithInvitationCodeCommand toCommand() {
        return new RegisterWithInvitationCodeCommand(code, name, rawPassword, email);
    }
}
