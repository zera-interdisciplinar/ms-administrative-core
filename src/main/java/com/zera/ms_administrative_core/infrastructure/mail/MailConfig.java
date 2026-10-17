package com.zera.ms_administrative_core.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;

@Configuration
@EnableConfigurationProperties(PasswordResetProperties.class)
public class MailConfig {

    private static final String RESEND_BASE_URL = "https://api.resend.com";

    @Bean
    PasswordResetMailGateway resendMailGateway(
            @Value("${zera.mail.resend.api-key:}") String apiKey,
            @Value("${zera.mail.resend.from:}") String from) {

        RestClient restClient = RestClient.builder()
                .baseUrl(RESEND_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        return new ResendMailGateway(restClient, from);
    }
}
