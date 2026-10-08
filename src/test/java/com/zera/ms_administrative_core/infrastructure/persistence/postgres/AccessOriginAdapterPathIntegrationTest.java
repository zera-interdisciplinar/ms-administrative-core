package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.usecase.auth.Login;
import com.zera.ms_administrative_core.core.usecase.auth.RefreshSession;
import com.zera.ms_administrative_core.core.usecase.auth.TokenPair;

/**
 * Prova a origem do DAU pelo caminho REAL: os beans de login e refresh, o {@code SET LOCAL} do
 * adaptador, o INSERT em refresh_token e o trigger {@code fn_registrar_acesso}, tudo no mesmo
 * Postgres.
 *
 * <p>Por que este teste existe: a versao anterior tinha testes verdes que chamavam
 * {@code set_config} direto no SQL. Eles provavam que o TRIGGER le a origem, mas nao que a
 * APLICACAO a publica. A aplicacao nunca publicava, todo refresh virava LOGIN, e nada falhava.
 * Este teste so passa se o Java realmente mandar a origem certa.
 */
@Transactional
class AccessOriginAdapterPathIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String SENHA = "senha-de-teste-123";

    private UUID ultimoUsuario;

    @Autowired
    private Login login;

    @Autowired
    private RefreshSession refreshSession;

    @Autowired
    private PasswordHasher passwordHasher;

    /**
     * O INSERT em refresh_token so chega ao Postgres no flush do JPA. Sem ele, o trigger de DAU
     * nunca dispara dentro deste teste -- o mesmo problema que apareceu no teste de auditoria.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /** Prepara um gestor ativo com senha real (bcrypt), para o login de verdade funcionar. */
    private String emailComSenhaReal() {
        Fixture f = novaHierarquia();
        String email = "origem." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        String hash = passwordHasher.hash(new RawPassword(SENHA)).hash();
        jdbc.update("UPDATE user_account SET email = ?, password = ? WHERE id = ?",
                email, hash, f.gestorId());
        ultimoUsuario = f.gestorId();
        return email;
    }

    private String origemDoUltimoAcesso() {
        return jdbc.queryForObject("""
                SELECT origem FROM user_access_log
                 WHERE user_id = ? ORDER BY id DESC LIMIT 1
                """, String.class, ultimoUsuario);
    }

    @Test
    @DisplayName("Login por senha deve gravar origem LOGIN em user_access_log")
    void loginRecordsLoginOrigin() {
        String email = emailComSenhaReal();

        login.execute(email, SENHA);
        entityManager.flush();

        assertThat(origemDoUltimoAcesso()).isEqualTo("LOGIN");
    }

    /**
     * O ponto que estava quebrado: o refresh emite uma sessao nova pelo MESMO caminho do login, e
     * antes do fix o trigger nao via nenhuma diferenca. Aqui a origem precisa ser REFRESH.
     */
    @Test
    @DisplayName("Renovacao de sessao deve gravar origem REFRESH, nao LOGIN")
    void refreshRecordsRefreshOrigin() {
        String email = emailComSenhaReal();
        TokenPair primeiraSessao = login.execute(email, SENHA);

        refreshSession.execute(primeiraSessao.refreshToken());
        entityManager.flush();

        assertThat(origemDoUltimoAcesso()).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("Login e refresh devem deixar duas origens distintas no log do mesmo usuario")
    void loginAndRefreshAreDistinguishedInHistory() {
        String email = emailComSenhaReal();
        TokenPair primeira = login.execute(email, SENHA);
        refreshSession.execute(primeira.refreshToken());
        entityManager.flush();

        var origens = jdbc.queryForList("""
                SELECT origem FROM user_access_log WHERE user_id = ? ORDER BY id
                """, String.class, ultimoUsuario);

        assertThat(origens).containsExactly("LOGIN", "REFRESH");
    }
}
