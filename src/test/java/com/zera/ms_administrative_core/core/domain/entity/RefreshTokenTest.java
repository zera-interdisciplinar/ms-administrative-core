package com.zera.ms_administrative_core.core.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void issueCreatesUsableToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.issue(userId, "hash", LocalDateTime.now().plusDays(7));

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isUsableAt(LocalDateTime.now())).isTrue();
    }

    @Test
    void revokedTokenIsNotUsable() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), "hash", LocalDateTime.now().plusDays(7));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isUsableAt(LocalDateTime.now())).isFalse();
    }

    @Test
    void expiredTokenIsNotUsable() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash",
                LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusDays(7), false);

        assertThat(token.isUsableAt(LocalDateTime.now())).isFalse();
    }
}
