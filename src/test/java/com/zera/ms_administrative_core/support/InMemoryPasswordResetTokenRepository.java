package com.zera.ms_administrative_core.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.core.repository.PasswordResetTokenRepository;

public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final Map<UUID, PasswordResetToken> byId = new LinkedHashMap<>();

    @Override
    public void save(PasswordResetToken token) {
        byId.put(token.getId(), token);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return byId.values().stream()
                .filter(t -> t.getTokenHash().equals(tokenHash))
                .findFirst();
    }

    public List<PasswordResetToken> all() {
        return List.copyOf(byId.values());
    }
}
