package com.zera.ms_administrative_core.infrastructure.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.usecase.auth.AccessTokenIssuer;
import com.zera.ms_administrative_core.core.usecase.auth.AuthenticatedUser;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final String keyId;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties,
            RsaKeyProvider keyProvider) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.keyId = keyProvider.keyId();
    }

    @Override
    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .subject(user.userId().toString())
                .claim("email", user.email())
                .claim("role", user.role().name());
        // o ms-inventory grava o "Cadastrado por" a partir deste claim; sem ele o createdByName
        // do item fica null. O builder do Spring rejeita claim nulo, entao so entra se houver nome.
        if (user.name() != null && !user.name().isBlank()) {
            claims.claim("name", user.name());
        }
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    @Override
    public Duration timeToLive() {
        return properties.accessTokenTtl();
    }
}
