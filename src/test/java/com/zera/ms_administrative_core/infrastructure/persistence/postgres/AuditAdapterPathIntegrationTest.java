package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Prova o CAMINHO COMPLETO da auditoria de autor: token -> {@code CurrentUserProvider} ->
 * {@code AuditContextBinder} -> {@code SET LOCAL} -> escrita JPA -> trigger.
 *
 * <p>POR QUE ESTE TESTE EXISTE SEPARADO DO {@link AuditTriggerIntegrationTest}: la o
 * {@code set_config} e o {@code INSERT} sao feitos pelo mesmo {@code JdbcTemplate} do teste, o que
 * prova a TRIGGER e mais nada. O elo que pode realmente quebrar em producao e outro: o
 * {@code JdbcTemplate} do binder e o Hibernate do {@code save} precisam compartilhar a MESMA
 * conexao, o que so acontece porque ha um unico {@code DataSource} e o {@code JpaTransactionManager}
 * publica o handle JDBC da transacao.
 *
 * <p>Se alguem introduzir um segundo {@code DataSource} ou trocar o gerenciador de transacao, esse
 * elo quebra EM SILENCIO -- a aplicacao continua funcionando e a auditoria passa a gravar autor
 * nulo. Este e o unico teste que pegaria isso.
 */
@Transactional
class AuditAdapterPathIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrganizationRepository organizations;

    /**
     * O flush explicito e necessario, e e didatico: o {@code save()} do JPA so agenda a escrita, que
     * o Hibernate envia ao banco no flush (normalmente no commit). Como o teste roda numa transacao
     * que sofre rollback, sem o flush o INSERT nunca chega ao Postgres, a trigger nunca dispara e
     * nao ha o que auditar. Ele tambem prova o ponto central: o {@code SET LOCAL} feito no inicio do
     * {@code save()} continua valendo no momento do flush, porque os dois estao na mesma transacao.
     */
    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(UUID usuarioId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    private Organization novaOrganizacao() {
        return new Organization(UUID.randomUUID(), "Org auditada", new Cnpj(cnpjSintetico()),
                Status.ACTIVE, new Email("auditada." + UUID.randomUUID() + "@test.local"),
                Plan.FREE, LocalDateTime.now().withNano(0), LocalDateTime.now().withNano(0));
    }

    @Test
    @DisplayName("Escrita pelo adaptador deve gravar o usuario do token na auditoria")
    void shouldRecordAuthenticatedUserThroughTheAdapter() {
        UUID autor = UUID.randomUUID();
        autenticarComo(autor);
        Organization organizacao = novaOrganizacao();

        organizations.save(organizacao);
        entityManager.flush();

        UUID usuarioApp = jdbc.queryForObject("""
                SELECT usuario_app FROM audit_log_organization
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, UUID.class, organizacao.getOrganizationId().toString());

        assertThat(usuarioApp)
                .as("o SET LOCAL do binder precisa alcancar a conexao usada pelo Hibernate")
                .isEqualTo(autor);
    }

    @Test
    @DisplayName("Exclusao pelo adaptador tambem deve registrar o autor")
    void shouldRecordAuthorOnDelete() {
        UUID autor = UUID.randomUUID();
        autenticarComo(autor);
        Organization organizacao = novaOrganizacao();
        organizations.save(organizacao);
        entityManager.flush();

        organizations.delete(organizacao.getOrganizationId());
        entityManager.flush();

        UUID usuarioApp = jdbc.queryForObject("""
                SELECT usuario_app FROM audit_log_organization
                 WHERE registro_id = ? AND operacao = 'DELETE' ORDER BY id DESC LIMIT 1
                """, UUID.class, organizacao.getOrganizationId().toString());

        assertThat(usuarioApp).isEqualTo(autor);
    }

    /**
     * Sem pessoa autenticada, {@code usuario_app} nulo e a informacao CORRETA: a mudanca nao partiu
     * de alguem. O que nao pode acontecer e a escrita falhar por falta de contexto.
     */
    @Test
    @DisplayName("Sem autenticacao a escrita passa e o autor fica nulo")
    void shouldWriteWithoutAuthorWhenAnonymous() {
        SecurityContextHolder.clearContext();
        Organization organizacao = novaOrganizacao();

        organizations.save(organizacao);
        entityManager.flush();

        Object usuarioApp = jdbc.queryForMap("""
                SELECT usuario_app FROM audit_log_organization
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, organizacao.getOrganizationId().toString()).get("usuario_app");

        assertThat(usuarioApp).isNull();
    }

    /** Token de servico autentica um CLIENTE, nao uma pessoa: o `sub` nao e UUID. */
    @Test
    @DisplayName("Token de servico nao deve virar autor de auditoria")
    void shouldNotRecordServiceClientAsAuthor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ms-inventory", "n/a",
                        List.of(new SimpleGrantedAuthority("SCOPE_maintenance:write"))));
        Organization organizacao = novaOrganizacao();

        organizations.save(organizacao);
        entityManager.flush();

        Object usuarioApp = jdbc.queryForMap("""
                SELECT usuario_app FROM audit_log_organization
                 WHERE registro_id = ? ORDER BY id DESC LIMIT 1
                """, organizacao.getOrganizationId().toString()).get("usuario_app");

        assertThat(usuarioApp).isNull();
    }
}
