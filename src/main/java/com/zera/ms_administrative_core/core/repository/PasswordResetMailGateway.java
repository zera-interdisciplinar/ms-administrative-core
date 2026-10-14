package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.valueobject.PasswordResetEmail;

public interface PasswordResetMailGateway {
    /**
     * Best-effort: implementacoes nunca devem propagar excecao daqui. O pedido de recuperacao ja
     * responde 202 antes de saber se o e-mail foi entregue -- uma falha aqui so e logada.
     */
    void send(PasswordResetEmail email);
}
