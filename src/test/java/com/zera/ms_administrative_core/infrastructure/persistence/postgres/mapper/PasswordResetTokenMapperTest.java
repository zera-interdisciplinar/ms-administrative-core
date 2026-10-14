package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.PasswordResetTokenJpa;

class PasswordResetTokenMapperTest {

    private final PasswordResetTokenMapper mapper = new PasswordResetTokenMapper();

    @Test
    void roundTripsAllFields() {
        PasswordResetToken domain = new PasswordResetToken(UUID.randomUUID(), UUID.randomUUID(),
                "a".repeat(64), LocalDateTime.now().plusMinutes(30).withNano(0),
                LocalDateTime.now().withNano(0), LocalDateTime.now().withNano(0));

        PasswordResetTokenJpa jpa = mapper.toJpa(domain);
        PasswordResetToken back = mapper.toDomain(jpa);

        assertThat(back.getId()).isEqualTo(domain.getId());
        assertThat(back.getUserId()).isEqualTo(domain.getUserId());
        assertThat(back.getTokenHash()).isEqualTo(domain.getTokenHash());
        assertThat(back.getExpiresAt()).isEqualTo(domain.getExpiresAt());
        assertThat(back.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(back.getUsedAt()).isEqualTo(domain.getUsedAt());
    }
}
