package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.DeviceTokenJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.DeviceTokenMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenRepositoryImplTest {

    @Mock
    private DeviceTokenJpaRepository jpa;

    // mapper real, de proposito: exercita DeviceTokenJpa/DeviceToken junto com o repository,
    // em vez de mockar e deixar as classes de persistencia sem nenhuma cobertura.
    private final DeviceTokenMapper mapper = new DeviceTokenMapper();

    private DeviceTokenRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new DeviceTokenRepositoryImpl(jpa, mapper);
        org.mockito.Mockito.lenient().when(jpa.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Deve inserir um token novo quando ele ainda nao existe")
    void shouldInsertNewTokenWhenNotFound() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        DeviceToken incoming = new DeviceToken(UUID.randomUUID(), userId, "token-abc", DevicePlatform.ANDROID, now, now);
        when(jpa.findByToken("token-abc")).thenReturn(Optional.empty());

        DeviceToken saved = repository.upsert(incoming);

        assertEquals(userId, saved.getUserId());
        assertEquals("token-abc", saved.getToken());
        assertEquals(DevicePlatform.ANDROID, saved.getPlatform());
    }

    @Test
    @DisplayName("Deve reatribuir o token a outro usuario quando ele ja existe (reinstalacao/troca de login)")
    void shouldReassignExistingTokenToNewUser() {
        UUID oldUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        DeviceTokenJpa existingJpa = new DeviceTokenJpa(
                UUID.randomUUID(), oldUserId, "token-abc", DevicePlatform.IOS, now, now);
        when(jpa.findByToken("token-abc")).thenReturn(Optional.of(existingJpa));

        DeviceToken incoming = new DeviceToken(UUID.randomUUID(), newUserId, "token-abc", DevicePlatform.ANDROID, now, now);

        DeviceToken result = repository.upsert(incoming);

        assertEquals(newUserId, result.getUserId());
        assertEquals(DevicePlatform.ANDROID, result.getPlatform());
    }

    @Test
    @DisplayName("Deve listar os tokens de um usuario")
    void shouldFindByUserId() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        DeviceTokenJpa jpaEntity = new DeviceTokenJpa(UUID.randomUUID(), userId, "token-abc", DevicePlatform.WEB, now, now);
        when(jpa.findAllByUserId(userId)).thenReturn(List.of(jpaEntity));

        List<DeviceToken> result = repository.findByUserId(userId);

        assertEquals(1, result.size());
        assertEquals("token-abc", result.get(0).getToken());
    }

    @Test
    @DisplayName("Deve delegar a remocao por token ao repositorio JPA")
    void shouldDeleteByToken() {
        repository.deleteByToken("token-abc");

        verify(jpa).deleteByToken(eq("token-abc"));
    }
}
