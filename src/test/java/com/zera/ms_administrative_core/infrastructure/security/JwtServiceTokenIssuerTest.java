package com.zera.ms_administrative_core.infrastructure.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.exception.InvalidServiceCredentialsException;
import com.zera.ms_administrative_core.core.usecase.auth.ServiceToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTokenIssuerTest {

    private static ServiceClientProperties properties() {
        return new ServiceClientProperties(Duration.ofMinutes(15), Map.of(
                "ms-inventory", new ServiceClientProperties.Client("segredo", List.of("notifications:write"))));
    }

    private JwtServiceTokenIssuer issuer(ServiceClientProperties clients) {
        RsaKeyProvider keys = new RsaKeyProvider(new JwtProperties(null, null, null, null, null));
        return new JwtServiceTokenIssuer(new SecurityConfig().jwtEncoder(keys),
                new JwtProperties(null, null, null, null, null), clients, keys);
    }

    @Test
    @DisplayName("Deve emitir o token com o escopo do cliente e sem role")
    void shouldIssueATokenWithTheClientScope() {
        ServiceToken token = issuer(properties()).execute("ms-inventory", "segredo");

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresInSeconds()).isEqualTo(900);

        Map<String, Object> claims = claimsOf(token.accessToken());
        assertThat(claims.get("scope")).isEqualTo("notifications:write");
        assertThat(claims.get("sub")).isEqualTo("ms-inventory");
        // token de servico nao carrega papel, entao nao alcanca rota de gestor
        assertThat(claims).doesNotContainKey("role");
        assertThat(claims.get("iss")).isEqualTo("ms-administrative-core");
    }

    @Test
    @DisplayName("Deve recusar cliente desconhecido e segredo errado")
    void shouldRejectUnknownClientAndWrongSecret() {
        JwtServiceTokenIssuer issuer = issuer(properties());

        assertThatThrownBy(() -> issuer.execute("nao-existe", "segredo"))
                .isInstanceOf(InvalidServiceCredentialsException.class);
        assertThatThrownBy(() -> issuer.execute("ms-inventory", "errado"))
                .isInstanceOf(InvalidServiceCredentialsException.class);
        assertThatThrownBy(() -> issuer.execute(null, null))
                .isInstanceOf(InvalidServiceCredentialsException.class);
    }

    /** Cliente sem segredo configurado nao autentica: ambiente sem o secret nao libera a rota. */
    @Test
    @DisplayName("Deve recusar cliente com segredo em branco na configuracao")
    void shouldRejectAClientWithoutConfiguredSecret() {
        ServiceClientProperties semSegredo = new ServiceClientProperties(Duration.ofMinutes(5), Map.of(
                "ms-inventory", new ServiceClientProperties.Client("", List.of("notifications:write"))));

        assertThatThrownBy(() -> issuer(semSegredo).execute("ms-inventory", ""))
                .isInstanceOf(InvalidServiceCredentialsException.class);
    }

    @Test
    @DisplayName("Deve aplicar os padroes quando a configuracao vem vazia")
    void shouldApplyDefaults() {
        ServiceClientProperties vazio = new ServiceClientProperties(null, null);

        assertThat(vazio.tokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(vazio.clients()).isEmpty();
        assertThat(vazio.authenticate("x", "y")).isEmpty();
        assertThat(new ServiceClientProperties.Client("s", null).scopes()).isEmpty();
    }

    private static Map<String, Object> claimsOf(String jwt) {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        return new tools.jackson.databind.ObjectMapper().readValue(payload, Map.class);
    }
}
