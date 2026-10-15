package com.zera.ms_administrative_core.infrastructure.mail;

import java.util.List;

/** Corpo minimo da API REST do Resend (POST /emails). Campos que nao usamos ficam de fora. */
class ResendDto {

    record SendEmailRequest(String from, List<String> to, String subject, String html) {
    }

    private ResendDto() {
    }
}
