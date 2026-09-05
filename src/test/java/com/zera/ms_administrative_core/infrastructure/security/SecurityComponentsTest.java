package com.zera.ms_administrative_core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.usecase.auth.AuthenticatedUser;

class SecurityComponentsTest {

    private static final String ISSUER = "ms-administrative-core";

    private static JwtProperties propertiesWithPem() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            String priv = pem("PRIVATE KEY", pair.getPrivate().getEncoded());
            String pub = pem("PUBLIC KEY", pair.getPublic().getEncoded());
            return new JwtProperties(ISSUER, Duration.ofMinutes(15), Duration.ofDays(7), priv, pub);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    @Test
    void jwtPropertiesFillDefaultsWhenBlank() {
        JwtProperties props = new JwtProperties("  ", null, null, null, null);

        assertThat(props.issuer()).isEqualTo("ms-administrative-core");
        assertThat(props.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(props.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void rsaKeyProviderGeneratesEphemeralKeysWhenNoneConfigured() {
        RsaKeyProvider provider = new RsaKeyProvider(
                new JwtProperties(ISSUER, null, null, "", ""));

        assertThat(provider.publicKey()).isInstanceOf(RSAPublicKey.class);
        assertThat(provider.privateKey()).isInstanceOf(RSAPrivateKey.class);
        assertThat(provider.keyId()).isNotBlank();
    }

    @Test
    void rsaKeyProviderParsesConfiguredPem() {
        RsaKeyProvider provider = new RsaKeyProvider(propertiesWithPem());

        assertThat(provider.publicKey().getModulus())
                .isEqualTo(((RSAPrivateKey) provider.privateKey()).getModulus());
    }

    @Test
    void rsaKeyProviderRejectsGarbagePem() {
        JwtProperties bad = new JwtProperties(ISSUER, null, null,
                "-----BEGIN PRIVATE KEY-----\nnot-base64!!!\n-----END PRIVATE KEY-----", "also-bad");

        assertThatThrownBy(() -> new RsaKeyProvider(bad)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void accessTokenIsSignedAndVerifiableWithExpectedClaims() {
        JwtProperties props = propertiesWithPem();
        RsaKeyProvider keys = new RsaKeyProvider(props);

        RSAKey jwk = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey()).keyID(keys.keyId()).build();
        JwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk)));
        JwtDecoder decoder = buildDecoder(keys);

        JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(encoder, props, keys);
        UUID userId = UUID.randomUUID();

        String token = issuer.issue(new AuthenticatedUser(userId, "alice@empresa.com", Role.MANAGER));
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("MANAGER");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("alice@empresa.com");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(decoded.getExpiresAt()).isNotNull();
        assertThat(issuer.timeToLive()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void decoderRejectsTokenFromAnotherIssuer() {
        JwtProperties props = new JwtProperties("someone-else",
                Duration.ofMinutes(15), Duration.ofDays(7), null, null);
        RsaKeyProvider keys = new RsaKeyProvider(props);
        RSAKey jwk = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey()).keyID(keys.keyId()).build();
        JwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk)));
        String foreignToken = new JwtAccessTokenIssuer(encoder, props, keys)
                .issue(new AuthenticatedUser(UUID.randomUUID(), "x@y.com", Role.EMPLOYEE));

        JwtDecoder strictDecoder = buildDecoder(keys);

        assertThatThrownBy(() -> strictDecoder.decode(foreignToken))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
    }

    private static JwtDecoder buildDecoder(RsaKeyProvider keys) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Test
    void sha256RefreshTokenGeneratorProducesHighEntropyValuesAndStableHash() {
        Sha256RefreshTokenGenerator generator = new Sha256RefreshTokenGenerator(
                new JwtProperties(ISSUER, null, Duration.ofDays(3), null, null));

        String a = generator.newRawToken();
        String b = generator.newRawToken();

        assertThat(a).isNotEqualTo(b).hasSizeGreaterThanOrEqualTo(43);
        assertThat(generator.hash(a)).hasSize(64).isEqualTo(generator.hash(a));
        assertThat(generator.hash(a)).isNotEqualTo(generator.hash(b));
        assertThat(generator.timeToLive()).isEqualTo(Duration.ofDays(3));
    }
}
