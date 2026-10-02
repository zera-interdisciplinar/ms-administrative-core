package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.repository.AuditContextRepository;
import com.zera.ms_administrative_core.infrastructure.security.CurrentUserProvider;

/**
 * Liga o usuario da requisicao a trigger de auditoria.
 *
 * <p>Chamado pelos adaptadores que escrevem em tabela auditada, ANTES da escrita e dentro da mesma
 * transacao -- o parametro de sessao e {@code SET LOCAL} e morre no commit.
 *
 * <p>Sem requisicao de usuario (job, agendador, token de servico) nao ha nada para publicar e a
 * auditoria grava {@code usuario_app} nulo, o que e a informacao correta: a mudanca nao partiu de
 * uma pessoa.
 */
@Component
public class AuditContextBinder {

    private final AuditContextRepository auditContext;
    private final CurrentUserProvider currentUser;

    public AuditContextBinder(AuditContextRepository auditContext, CurrentUserProvider currentUser) {
        this.auditContext = auditContext;
        this.currentUser = currentUser;
    }

    public void bindCurrentUser() {
        currentUser.currentUserId().ifPresent(auditContext::bindUser);
    }
}
