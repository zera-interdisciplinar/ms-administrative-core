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

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.PasswordResetTokenJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.PasswordResetTokenMapper;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenRepositoryImplTest {

    @Mock
    private PasswordResetTokenJpaRepository jpa;

    private PasswordResetTokenRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PasswordResetTokenRepositoryImpl(jpa, new PasswordResetTokenMapper());
    }

    @Test
    void savePersistsMappedEntity() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "h".repeat(64),
                LocalDateTime.now().plusMinutes(30));

        repository.save(token);

        ArgumentCaptor<PasswordResetTokenJpa> captor = ArgumentCaptor.forClass(PasswordResetTokenJpa.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(token.getTokenHash());
    }

    @Test
    void findByTokenHashMapsToDomain() {
        UUID id = UUID.randomUUID();
        PasswordResetTokenJpa jpaEntity = new PasswordResetTokenJpa(id, UUID.randomUUID(), "x".repeat(64),
                LocalDateTime.now().plusMinutes(30), null, LocalDateTime.now());
        when(jpa.findByTokenHash("x".repeat(64))).thenReturn(Optional.of(jpaEntity));

        Optional<PasswordResetToken> result = repository.findByTokenHash("x".repeat(64));

        assertThat(result).get().extracting(PasswordResetToken::getId).isEqualTo(id);
    }

    @Test
    void findByTokenHashReturnsEmptyWhenAbsent() {
        when(jpa.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(repository.findByTokenHash("nope")).isEmpty();
    }
}
