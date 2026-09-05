package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RefreshTokenJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.RefreshTokenMapper;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    @Mock
    private RefreshTokenJpaRepository jpa;

    private RefreshTokenRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RefreshTokenRepositoryImpl(jpa, new RefreshTokenMapper());
    }

    @Test
    void savePersistsMappedEntity() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), "h".repeat(64),
                LocalDateTime.now().plusDays(7));

        repository.save(token);

        ArgumentCaptor<RefreshTokenJpa> captor = ArgumentCaptor.forClass(RefreshTokenJpa.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(token.getTokenHash());
    }

    @Test
    void findByTokenHashMapsToDomain() {
        UUID id = UUID.randomUUID();
        RefreshTokenJpa jpaEntity = new RefreshTokenJpa(id, UUID.randomUUID(), "x".repeat(64),
                LocalDateTime.now().plusDays(1), false, LocalDateTime.now());
        when(jpa.findByTokenHash("x".repeat(64))).thenReturn(Optional.of(jpaEntity));

        Optional<RefreshToken> result = repository.findByTokenHash("x".repeat(64));

        assertThat(result).get().extracting(RefreshToken::getId).isEqualTo(id);
    }

    @Test
    void findByTokenHashReturnsEmptyWhenAbsent() {
        when(jpa.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(repository.findByTokenHash("nope")).isEmpty();
    }

    @Test
    void revokeAllForUserDelegates() {
        UUID userId = UUID.randomUUID();

        repository.revokeAllForUser(userId);

        verify(jpa).revokeAllForUser(userId);
    }
}
