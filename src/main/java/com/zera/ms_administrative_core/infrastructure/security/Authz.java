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

    /**
     * Rotas internas, chamadas por outro servico com token de servico. Token de usuario nao traz
     * {@code scope}, entao nao alcanca estas rotas.
     */
    public static final String SERVICE_NOTIFICATIONS = "hasAuthority('SCOPE_notifications:write')";

    /**
     * Listagem de usuarios: o gestor logado (tela/card) OU um servico com o escopo dedicado. O
     * ms-inventory usa essa rota (GET /users?role=MANAGER&unitId=) no motor de regras antigo
     * (garantia, estoque, etc.) para achar um gestor qualquer da unidade -- com token de servico,
     * que nao tem {@code role}, {@code hasRole('MANAGER')} sozinho sempre devolvia 403.
     *
     * <p>Nao usar para achar o gestor de um funcionario especifico: "um gestor qualquer da
     * unidade" nao e o mesmo que "o gestor daquele funcionario" quando a unidade tem mais de um
     * MANAGER -- ver {@link #SELF_OR_MANAGER_OR_SERVICE_USERS_READ}.
     */
    public static final String MANAGER_OR_SERVICE_USERS_READ =
            "hasRole('MANAGER') or hasAuthority('SCOPE_users:read')";

    /**
     * {@link #SELF_OR_MANAGER}, mas tambem aceita servico com o escopo dedicado. O ms-inventory
     * usa GET /users/{id} com o id de quem cadastrou o item, pra ler o {@code managerId} real
     * daquele funcionario e notificar o gestor certo (nao "um gestor qualquer da unidade", que
     * falha quando ha mais de um MANAGER na mesma unidade).
     */
    public static final String SELF_OR_MANAGER_OR_SERVICE_USERS_READ =
            "hasRole('MANAGER') or #id.toString() == authentication.name or hasAuthority('SCOPE_users:read')";

    /**
     * Operacoes de manutencao do banco (procedures). Aceita gestor autenticado OU token de servico
     * com o escopo dedicado, porque o mesmo acionamento serve ao painel e a um agendador externo.
     * O escopo e separado de {@code notifications:write} de proposito: quem pode entregar alerta
     * nao deveria, por isso, poder fechar alerta em massa.
     */
    public static final String MANAGER_OR_SERVICE_MAINTENANCE =
            "hasRole('MANAGER') or hasAuthority('SCOPE_maintenance:write')";

    private Authz() {}
}
