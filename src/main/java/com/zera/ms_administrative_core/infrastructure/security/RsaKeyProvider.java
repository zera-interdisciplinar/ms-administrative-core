package com.zera.ms_administrative_core.infrastructure.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fornece o par de chaves RSA usado para assinar (privada) e validar (publica) os access tokens.
 *
 * <p>As chaves vem de {@link JwtProperties} em formato PEM. Se nao forem informadas, um par efemero
 * de 2048 bits e gerado no boot — util apenas para desenvolvimento local, pois cada reinicio
 * invalida os tokens ja emitidos e o Kong nao conseguira validar.
 */
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(JwtProperties properties) {
        boolean hasKeys = notBlank(properties.privateKey()) && notBlank(properties.publicKey());
        if (hasKeys) {
            this.privateKey = parsePrivateKey(properties.privateKey());
            this.publicKey = parsePublicKey(properties.publicKey());
        } else {
            log.warn("Nenhuma chave RSA configurada (zera.jwt.private-key / zera.jwt.public-key). "
                    + "Gerando par efemero — NAO usar em QA/producao.");
            KeyPair generated = generateKeyPair();
            this.privateKey = (RSAPrivateKey) generated.getPrivate();
            this.publicKey = (RSAPublicKey) generated.getPublic();
        }
        this.keyId = thumbprint(this.publicKey);
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    /** Identificador estavel da chave (SHA-256 do encoding), usado como {@code kid} no JWK/JWT. */
    public String keyId() {
        return keyId;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        byte[] der = decodePem(pem, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Chave privada RSA invalida (esperado PKCS#8 PEM)", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        byte[] der = decodePem(pem, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Chave publica RSA invalida (esperado X.509/SPKI PEM)", e);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA indisponivel na JVM", e);
        }
    }

    private static String thumbprint(RSAPublicKey key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
