package com.zera.ms_administrative_core.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DefaultPasswordResetLinkFactoryTest {

    @Test
    void buildsDeepLinkWithToken() {
        var factory = new DefaultPasswordResetLinkFactory(
                new PasswordResetProperties("zera://reset-password", Duration.ofMinutes(30)));

        assertThat(factory.buildLink("raw-token"))
                .isEqualTo("zera://reset-password?token=raw-token");
    }

    @Test
    void appendsTokenWithAmpersandWhenBaseAlreadyHasQuery() {
        var factory = new DefaultPasswordResetLinkFactory(
                new PasswordResetProperties("zera://reset-password?source=email", Duration.ofMinutes(30)));

        assertThat(factory.buildLink("raw-token"))
                .isEqualTo("zera://reset-password?source=email&token=raw-token");
    }

    @Test
    void fallsBackToDefaultSchemeWhenBlank() {
        var factory = new DefaultPasswordResetLinkFactory(
                new PasswordResetProperties("", Duration.ofMinutes(30)));

        assertThat(factory.buildLink("raw-token"))
                .isEqualTo("zera://reset-password?token=raw-token");
    }
}
