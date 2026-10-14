package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

public interface RequestPasswordReset {
    /**
     * Nunca revela se o e-mail existe: quem chama sempre recebe sucesso, o efeito (enviar ou nao
     * o e-mail) e interno.
     */
    void execute(String email);
}
