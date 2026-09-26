package com.zera.ms_administrative_core.infrastructure.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.exception.InvalidServiceCredentialsException;
import com.zera.ms_administrative_core.core.usecase.auth.IssueServiceToken;
import com.zera.ms_administrative_core.core.usecase.auth.ServiceToken;

/**
 * Emite o token de servico com o mesmo par de chaves do token de usuario, entao os servicos que ja
 * validam pelo JWKS nao mudam nada. A diferenca esta nos claims: o token de servico traz
 * {@code scope} e nao traz {@code role}, entao ele nao alcanca as rotas de gestor, e o token de
 * usuario nao alcanca as rotas internas.
 */
@Service
public class JwtServiceTokenIssuer implements IssueServiceToken {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final ServiceClientProperties serviceClients;
    private final String keyId;

    public JwtServiceTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties,
            ServiceClientProperties serviceClients, RsaKeyProvider keyProvider) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.serviceClients = serviceClients;
        this.keyId = keyProvider.keyId();
    }

    @Override
    public ServiceToken execute(String clientId, String clientSecret) {
        ServiceClientProperties.Client client = serviceClients.authenticate(clientId, clientSecret)
                .orElseThrow(InvalidServiceCredentialsException::new);

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(serviceClients.tokenTtl()))
                .subject(clientId)
                .claim("scope", String.join(" ", client.scopes()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();

        return new ServiceToken(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                "Bearer", serviceClients.tokenTtl().toSeconds());
    }
}
