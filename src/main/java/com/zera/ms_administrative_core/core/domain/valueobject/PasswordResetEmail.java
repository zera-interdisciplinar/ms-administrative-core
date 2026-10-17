package com.zera.ms_administrative_core.core.domain.valueobject;

/** Conteudo do e-mail de recuperacao de senha, montado no caso de uso e entregue ao gateway. */
public record PasswordResetEmail(String to, String recipientName, String resetLink) {
}
