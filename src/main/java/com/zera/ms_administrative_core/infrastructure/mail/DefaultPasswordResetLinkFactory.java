package com.zera.ms_administrative_core.infrastructure.mail;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.usecase.auth.passwordReset.PasswordResetLinkFactory;

@Component
public class DefaultPasswordResetLinkFactory implements PasswordResetLinkFactory {

    private final String deepLinkBase;

    public DefaultPasswordResetLinkFactory(PasswordResetProperties properties) {
        this.deepLinkBase = properties.deepLinkBase();
    }

    @Override
    public String buildLink(String rawToken) {
        String separator = deepLinkBase.contains("?") ? "&" : "?";
        return deepLinkBase + separator + "token=" + rawToken;
    }
}
