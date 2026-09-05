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
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .subject(user.userId().toString())
                .claim("email", user.email())
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public Duration timeToLive() {
        return properties.accessTokenTtl();
    }
}
