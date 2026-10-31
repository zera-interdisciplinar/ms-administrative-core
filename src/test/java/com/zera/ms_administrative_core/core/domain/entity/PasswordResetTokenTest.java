package com.zera.ms_administrative_core.core.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    @Test
    void issueCreatesUsableToken() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.issue(userId, "hash", LocalDateTime.now().plusMinutes(30));

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.isUsableAt(LocalDateTime.now())).isTrue();
    }

    @Test
    void usedTokenIsNotUsable() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "hash",
                LocalDateTime.now().plusMinutes(30));

        token.markUsed();

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isUsableAt(LocalDateTime.now())).isFalse();
    }

    @Test
    void expiredTokenIsNotUsable() {
        PasswordResetToken token = new PasswordResetToken(UUID.randomUUID(), UUID.randomUUID(), "hash",
                LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusMinutes(30), null);

        assertThat(token.isUsableAt(LocalDateTime.now())).isFalse();
    }
}
