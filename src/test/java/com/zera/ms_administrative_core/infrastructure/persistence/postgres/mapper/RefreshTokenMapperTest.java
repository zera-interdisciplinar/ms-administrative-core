package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RefreshTokenJpa;

class RefreshTokenMapperTest {

    private final RefreshTokenMapper mapper = new RefreshTokenMapper();

    @Test
    void roundTripsAllFields() {
        RefreshToken domain = new RefreshToken(UUID.randomUUID(), UUID.randomUUID(),
                "a".repeat(64), LocalDateTime.now().plusDays(7).withNano(0),
                LocalDateTime.now().withNano(0), true);

        RefreshTokenJpa jpa = mapper.toJpa(domain);
        RefreshToken back = mapper.toDomain(jpa);

        assertThat(back.getId()).isEqualTo(domain.getId());
        assertThat(back.getUserId()).isEqualTo(domain.getUserId());
        assertThat(back.getTokenHash()).isEqualTo(domain.getTokenHash());
        assertThat(back.getExpiresAt()).isEqualTo(domain.getExpiresAt());
        assertThat(back.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(back.isRevoked()).isTrue();
    }
}
