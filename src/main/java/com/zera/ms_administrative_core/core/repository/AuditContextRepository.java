package com.zera.ms_administrative_core.core.repository;

import java.util.UUID;

/**
 * Porta que informa ao banco QUEM esta agindo, para a trigger de auditoria registrar.
 *
 * <p>Existe porque {@code CURRENT_USER} nao responde essa pergunta: a aplicacao usa um unico
 * usuario de banco no pool, entao toda linha de auditoria teria o mesmo autor. O identificador do
 * usuario vem do {@code sub} do JWT e e publicado como parametro de sessao.
 */
public interface AuditContextRepository {

    /**
     * Publica o usuario na transacao corrente. Precisa rodar DENTRO da mesma transacao da escrita
     * auditada: e um {@code SET LOCAL}, que o banco descarta no commit.
     */
    void bindUser(UUID usuarioId);
}
