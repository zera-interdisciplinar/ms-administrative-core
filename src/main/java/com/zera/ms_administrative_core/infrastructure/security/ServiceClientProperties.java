package com.zera.ms_administrative_core.infrastructure.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clientes de servico autorizados a chamar as rotas internas. Ficam em configuracao, e nao no
 * banco, porque sao poucos e conhecidos no deploy: o segredo mora no secret do k8s, junto com as
 * demais credenciais do ambiente.
 *
 * <p>O segredo e comparado em texto porque ja vem de um secret; guardar hash aqui so mudaria onde
 * o valor sensivel esta, sem tirar o segredo do ambiente.
 */
@ConfigurationProperties(prefix = "zera.service-auth")
public record ServiceClientProperties(Duration tokenTtl, Map<String, Client> clients) {

    public record Client(String secret, List<String> scopes) {
        public Client {
            scopes = scopes == null ? List.of() : List.copyOf(scopes);
        }
    }

    public ServiceClientProperties {
        tokenTtl = tokenTtl == null ? Duration.ofMinutes(15) : tokenTtl;
        clients = clients == null ? Map.of() : Map.copyOf(clients);
    }

    /** Cliente com segredo conferido; vazio quando o id nao existe ou o segredo nao bate. */
    public Optional<Client> authenticate(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            return Optional.empty();
        }
        Client client = clients.get(clientId);
        if (client == null || client.secret() == null || client.secret().isBlank()) {
            return Optional.empty();
        }
        // comparacao de tempo constante: o segredo nao pode vazar pelo tempo de resposta
        boolean matches = java.security.MessageDigest.isEqual(
                client.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                clientSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return matches ? Optional.of(client) : Optional.empty();
    }
}
