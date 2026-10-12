package com.zera.ms_administrative_core.core.repository;

import java.util.List;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;

public interface DeviceTokenRepository {
    /** Por {@code token}: atualiza o {@code user_id} se o token ja existia em outra conta. */
    DeviceToken upsert(DeviceToken deviceToken);
    List<DeviceToken> findByUserId(UUID userId);
    void deleteByToken(String token);
}
