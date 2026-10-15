package com.zera.ms_administrative_core.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;

@SpringBootTest
@ActiveProfiles("test")
class MailConfigWiringTest {

    @Autowired
    private PasswordResetMailGateway mailGateway;

    @Test
    void registersResendClient() {
        assertThat(mailGateway).isInstanceOf(ResendMailGateway.class);
    }
}
