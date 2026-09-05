package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.zera.ms_administrative_core.infrastructure.security.RsaKeyProvider;

/**
 * Publica a chave publica de assinatura no formato JWKS para o Kong e o ms-inventory validarem
 * os access tokens sem compartilhar segredo.
 */
@RestController
public class JwksController {

    private final Map<String, Object> jwkSet;

    public JwksController(RsaKeyProvider keyProvider) {
        RSAKey publicJwk = new RSAKey.Builder(keyProvider.publicKey())
                .keyID(keyProvider.keyId())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256"))
                .build();
        this.jwkSet = new JWKSet(publicJwk).toJSONObject(true);
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwkSet;
    }
}
