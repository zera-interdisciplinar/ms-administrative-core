package com.zera.ms_administrative_core.infrastructure.mail;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.zera.ms_administrative_core.core.domain.valueobject.PasswordResetEmail;
import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;

class ResendMailGatewayTest {

    private MockRestServiceServer server;
    private PasswordResetMailGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new ResendMailGateway(builder.build(), "no-reply@zera.app");
    }

    @Test
    void postsEmailWithBearerTokenAndLinkInBody() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        gateway.send(new PasswordResetEmail("alice@empresa.com", "Alice",
                "https://app.test/reset-password?token=raw"));

        server.verify();
    }

    @Test
    void neverPropagatesExceptionOnFailure() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withServerError());

        gateway.send(new PasswordResetEmail("alice@empresa.com", "Alice",
                "https://app.test/reset-password?token=raw"));

        server.verify();
    }
}
