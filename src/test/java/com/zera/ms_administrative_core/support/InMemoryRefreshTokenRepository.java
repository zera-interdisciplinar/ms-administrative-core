package com.zera.ms_administrative_core.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<UUID, RefreshToken> byId = new LinkedHashMap<>();

    @Override
    public void save(RefreshToken token) {
        byId.put(token.getId(), token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return byId.values().stream()
                .filter(t -> t.getTokenHash().equals(tokenHash))
                .findFirst();
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        byId.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .forEach(RefreshToken::revoke);
    }

    public List<RefreshToken> all() {
        return new ArrayList<>(byId.values());
    }
}
