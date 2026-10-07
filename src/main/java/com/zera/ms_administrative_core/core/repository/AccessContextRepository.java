package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.valueobject.AccessOrigin;

/**
 * Porta que informa ao banco POR QUE uma sessao esta sendo emitida, para o trigger de DAU
 * (V11) gravar a origem certa em {@code user_access_log}.
 *
 * <p>Mesmo mecanismo do {@link AuditContextRepository}: o valor e publicado como parametro de
 * sessao e vale so na transacao corrente. Sem isso, todo acesso cairia no default LOGIN do trigger,
 * e a dimensao de origem da camada analitica ficaria sempre igual, sem ninguem perceber.
 */
public interface AccessContextRepository {

    /** Precisa rodar DENTRO da mesma transacao do INSERT em refresh_token. */
    void bindOrigin(AccessOrigin origin);
}
