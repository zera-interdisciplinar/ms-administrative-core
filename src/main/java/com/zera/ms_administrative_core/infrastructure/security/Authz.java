package com.zera.ms_administrative_core.infrastructure.security;

/**
 * Expressoes SpEL reutilizaveis para {@code @PreAuthorize}.
 *
 * <p>Regra da v1: operacoes de escrita/administrativas exigem {@code MANAGER}; leituras exigem
 * apenas autenticacao (garantido pelo {@code SecurityFilterChain}); operacoes de autoatendimento
 * podem ser feitas pelo dono da conta ou por um {@code MANAGER}.
 */
public final class Authz {

    /** Somente gestores. */
    public static final String MANAGER = "hasRole('MANAGER')";

    /**
     * O proprio usuario (o {@code sub} do token e igual ao path variable {@code id}) ou um gestor.
     * O metodo anotado precisa ter um parametro {@code UUID id}.
     */
    public static final String SELF_OR_MANAGER =
            "hasRole('MANAGER') or #id.toString() == authentication.name";

    private Authz() {}
}
