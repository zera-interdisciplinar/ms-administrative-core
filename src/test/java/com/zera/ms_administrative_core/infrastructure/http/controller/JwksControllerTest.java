package com.zera.ms_administrative_core.infrastructure.http.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.infrastructure.security.JwtProperties;
import com.zera.ms_administrative_core.infrastructure.security.RsaKeyProvider;

class JwksControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void exposesPublicRsaKeyWithoutPrivateMaterial() {
        RsaKeyProvider keys = new RsaKeyProvider(
                new JwtProperties("ms-administrative-core", Duration.ofMinutes(15),
                        Duration.ofDays(7), null, null));

        Map<String, Object> body = new JwksController(keys).jwks();

        List<Map<String, Object>> jwkList = (List<Map<String, Object>>) body.get("keys");
        assertThat(jwkList).hasSize(1);
        Map<String, Object> jwk = jwkList.get(0);
        assertThat(jwk).containsEntry("kty", "RSA").containsEntry("use", "sig");
        assertThat(jwk).containsKey("n").containsKey("e");
        assertThat(jwk).doesNotContainKey("d").doesNotContainKey("p").doesNotContainKey("q");
        assertThat(jwk.get("kid")).isEqualTo(keys.keyId());
    }
}
