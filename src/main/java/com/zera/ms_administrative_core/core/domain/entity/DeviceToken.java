package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;

public class DeviceToken {
    private final UUID id;
    private UUID userId;
    private final String token;
    private DevicePlatform platform;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DeviceToken(UUID id, UUID userId, String token, DevicePlatform platform, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** O token reaparece em outra conta no mesmo device (reinstalacao/troca de login). */
    public void reassignTo(UUID userId, DevicePlatform platform) {
        this.userId = userId;
        this.platform = platform;
        this.updatedAt = LocalDateTime.now();
    }
}
