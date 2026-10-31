package com.zera.ms_administrative_core.infrastructure.mail;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.zera.ms_administrative_core.core.domain.valueobject.PasswordResetEmail;
import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;
import com.zera.ms_administrative_core.infrastructure.mail.ResendDto.SendEmailRequest;

/**
 * Adaptador da porta {@link PasswordResetMailGateway} para a API REST do Resend
 * (https://resend.com/docs/api-reference/emails/send-email). Sem SDK: e um POST simples com Bearer
 * token, nao justifica mais uma dependencia.
 */
class ResendMailGateway implements PasswordResetMailGateway {

    private static final Logger log = LoggerFactory.getLogger(ResendMailGateway.class);

    private final RestClient restClient;
    private final String from;

    ResendMailGateway(RestClient restClient, String from) {
        this.restClient = restClient;
        this.from = from;
    }

    @Override
    public void send(PasswordResetEmail email) {
        try {
            restClient.post()
                    .uri("/emails")
                    .body(new SendEmailRequest(from, List.of(email.to()), "Recuperacao de senha",
                            buildHtml(email)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // Best-effort: a decisao de nao vazar existencia do e-mail ja foi tomada antes de
            // chegar aqui (sempre 202), entao uma falha de envio so pode ser logada.
            log.warn("Falha ao enviar e-mail de recuperacao de senha para {}", email.to(), ex);
        }
    }

    private String buildHtml(PasswordResetEmail email) {
        return "<p>Ola, " + email.recipientName() + ".</p>"
                + "<p>Recebemos um pedido para redefinir sua senha. Se foi voce, clique no link "
                + "abaixo (valido por tempo limitado):</p>"
                + "<p><a href=\"" + email.resetLink() + "\">" + email.resetLink() + "</a></p>"
                + "<p>Se voce nao pediu isso, ignore este e-mail.</p>";
    }
}
