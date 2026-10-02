-- V10__create_audit_log.sql
--
-- Auditoria de escrita por trigger. Tres decisoes que valem registro:
--
-- 1) HERANCA DE TABELAS. `audit_log` e a tabela pai e nunca recebe linha: cada tabela auditada
--    grava na propria filha. Um `SELECT FROM audit_log` continua vendo tudo (herenca do Postgres
--    faz a uniao), mas cada filha tem seus proprios indices e pode ser truncada/arquivada sozinha,
--    sem travar a auditoria das outras. Sem isso, a auditoria vira uma unica tabela quente que
--    cresce mais rapido que todo o resto do banco somado.
--
-- 2) `usuario_banco` (CURRENT_USER) e `usuario_app` sao COISAS DIFERENTES. A aplicacao usa um
--    unico usuario de banco no pool, entao CURRENT_USER responde "quem conectou", nunca "quem
--    agiu". Quem age vem do token e chega ate aqui pelo parametro de sessao `zera.app_user`,
--    setado com SET LOCAL pela aplicacao dentro da mesma transacao. As duas colunas existem de
--    proposito: uma serve para forense de infraestrutura, a outra para forense de produto.
--
-- 3) MASCARAMENTO. `to_jsonb(NEW)` copia a linha inteira, inclusive o hash bcrypt de senha. Uma
--    auditoria que guarda hash de senha em texto e um vazamento com nome bonito, entao as chaves
--    sensiveis sao removidas do JSONB antes de gravar.

CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    tabela        TEXT NOT NULL,
    operacao      TEXT NOT NULL CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE')),
    registro_id   TEXT NULL,
    dados_antigos JSONB NULL,
    dados_novos   JSONB NULL,
    usuario_banco TEXT NOT NULL,
    usuario_app   UUID NULL,
    ocorrido_em   TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL
);

COMMENT ON TABLE audit_log IS
    'Tabela pai da auditoria; permanece vazia. Cada tabela auditada grava na filha audit_log_<tabela>.';

-- Filhas: uma por tabela auditada. NAO herdam o PRIMARY KEY (o Postgres nao propaga constraints
-- de unicidade na heranca), mas compartilham a sequencia do BIGSERIAL do pai, entao o id segue
-- global e monotonico entre elas.
CREATE TABLE audit_log_user_account () INHERITS (audit_log);
CREATE TABLE audit_log_alert        () INHERITS (audit_log);
CREATE TABLE audit_log_organization () INHERITS (audit_log);

CREATE INDEX idx_audit_user_account_registro ON audit_log_user_account (registro_id, ocorrido_em DESC);
CREATE INDEX idx_audit_alert_registro        ON audit_log_alert        (registro_id, ocorrido_em DESC);
CREATE INDEX idx_audit_organization_registro ON audit_log_organization (registro_id, ocorrido_em DESC);

-- Chaves que nunca podem entrar no log, em qualquer tabela auditada hoje ou amanha.
CREATE OR REPLACE FUNCTION fn_mascarar_sensiveis(p_dados JSONB)
RETURNS JSONB
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN p_dados IS NULL THEN NULL
        ELSE p_dados - 'password' - 'token_hash' - 'code'
    END;
$$;

COMMENT ON FUNCTION fn_mascarar_sensiveis(JSONB) IS
    'Remove hash de senha, hash de refresh token e codigo de convite do payload de auditoria.';

-- SECURITY DEFINER com search_path FIXO. As duas coisas andam juntas:
--
-- SECURITY DEFINER e necessario para que a auditoria funcione mesmo se, um dia, existir um papel
-- que possa escrever em `user_account` sem poder escrever em `audit_log_*` -- sem ele, a trigger
-- falharia e derrubaria a escrita que deveria apenas registrar.
--
-- Mas SECURITY DEFINER sem search_path fixo e uma escalada de privilegio conhecida.
--
-- O ataque foi REPRODUZIDO neste schema: uma role comum, com apenas INSERT no dominio e SEM
-- superuser, executa
--     CREATE TEMP TABLE audit_log_user_account (LIKE public.audit_log_user_account);
--     INSERT INTO user_account (...);
-- e a linha de auditoria vai para a tabela temporaria do atacante, nao para a real. A trilha e
-- desviada em silencio -- o oposto exato da garantia deste arquivo.
--
-- `pg_temp` PRECISA ESTAR LISTADO, E POR ULTIMO. Esta e a parte contraintuitiva: quando pg_temp
-- NAO aparece no search_path, o Postgres o pesquisa PRIMEIRO para nomes de relacao. Ou seja,
-- `SET search_path = pg_catalog, public` sozinho NAO fecha o buraco -- parece que fecha, e nao
-- fecha. Listar pg_temp ao final e o que garante que a tabela real vem antes.
CREATE OR REPLACE FUNCTION fn_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
    v_antigos     JSONB;
    v_novos       JSONB;
    v_registro_id TEXT;
    v_usuario_app UUID;
    v_destino     TEXT;
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_antigos := fn_mascarar_sensiveis(to_jsonb(OLD));
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_novos := fn_mascarar_sensiveis(to_jsonb(NEW));
    END IF;

    v_registro_id := COALESCE(v_novos ->> 'id', v_antigos ->> 'id');

    -- current_setting com missing_ok=true devolve NULL quando a aplicacao nao setou nada (job,
    -- migracao, psql na mao). Um uuid invalido tambem nao pode derrubar a escrita que esta sendo
    -- auditada: nesse caso a auditoria grava sem o usuario de aplicacao.
    BEGIN
        v_usuario_app := NULLIF(current_setting('zera.app_user', true), '')::UUID;
    EXCEPTION WHEN invalid_text_representation THEN
        v_usuario_app := NULL;
    END;

    -- Se a tabela auditada nao tiver filha dedicada, cai no pai em vez de estourar erro: perder o
    -- particionamento e aceitavel, perder o registro de auditoria nao e.
    -- Qualificado com `public.` em to_regclass e no INSERT: defesa em profundidade, para o caso
    -- de alguem afrouxar o search_path acima no futuro.
    v_destino := 'audit_log_' || TG_TABLE_NAME;
    IF to_regclass('public.' || v_destino) IS NULL THEN
        v_destino := 'audit_log';
    END IF;

    EXECUTE format(
        'INSERT INTO public.%I (tabela, operacao, registro_id, dados_antigos, dados_novos,
                                usuario_banco, usuario_app, ocorrido_em)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)', v_destino)
    USING TG_TABLE_NAME, TG_OP, v_registro_id, v_antigos, v_novos,
          CURRENT_USER, v_usuario_app, LOCALTIMESTAMP(0);

    RETURN COALESCE(NEW, OLD);
END;
$$;

COMMENT ON FUNCTION fn_auditoria() IS
    'Trigger generica de auditoria: usa TG_OP/TG_TABLE_NAME e grava OLD/NEW como JSONB mascarado.';

CREATE TRIGGER trg_auditoria_user_account
    AFTER INSERT OR UPDATE OR DELETE ON user_account
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria();

CREATE TRIGGER trg_auditoria_alert
    AFTER INSERT OR UPDATE OR DELETE ON alert
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria();

CREATE TRIGGER trg_auditoria_organization
    AFTER INSERT OR UPDATE OR DELETE ON organization
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria();

-- ---------------------------------------------------------------------------------------------
-- TRUNCATE tambem precisa deixar rastro.
--
-- Trigger FOR EACH ROW nao dispara em TRUNCATE -- nao ha linhas para percorrer. Consequencia: um
-- `TRUNCATE alert` apagaria a tabela inteira sem uma unica linha na trilha, num objeto cujo
-- proposito declarado e auditar escrita. E o furo mais silencioso possivel, porque a auditoria
-- parece intacta: ela so nao tem nada sobre o maior evento destrutivo que ja aconteceu.
--
-- A trigger abaixo e STATEMENT-level: registra o comando, nao as linhas (elas ja se foram). Nao
-- substitui backup -- serve para responder "quando e por qual conexao isso aconteceu".
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auditoria_truncate()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
    v_usuario_app UUID;
    v_destino     TEXT;
BEGIN
    BEGIN
        v_usuario_app := NULLIF(current_setting('zera.app_user', true), '')::UUID;
    EXCEPTION WHEN invalid_text_representation THEN
        v_usuario_app := NULL;
    END;

    v_destino := 'audit_log_' || TG_TABLE_NAME;
    IF to_regclass('public.' || v_destino) IS NULL THEN
        v_destino := 'audit_log';
    END IF;

    EXECUTE format(
        'INSERT INTO public.%I (tabela, operacao, registro_id, dados_antigos, dados_novos,
                                usuario_banco, usuario_app, ocorrido_em)
         VALUES ($1, $2, NULL, NULL, NULL, $3, $4, $5)', v_destino)
    USING TG_TABLE_NAME, 'TRUNCATE', CURRENT_USER, v_usuario_app, LOCALTIMESTAMP(0);

    RETURN NULL;
END;
$$;

-- O CHECK de `operacao` precisa aceitar o novo valor.
ALTER TABLE audit_log DROP CONSTRAINT audit_log_operacao_check;
ALTER TABLE audit_log ADD CONSTRAINT audit_log_operacao_check
    CHECK (operacao IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE'));

CREATE TRIGGER trg_auditoria_truncate_user_account
    AFTER TRUNCATE ON user_account
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_truncate();

CREATE TRIGGER trg_auditoria_truncate_alert
    AFTER TRUNCATE ON alert
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_truncate();

CREATE TRIGGER trg_auditoria_truncate_organization
    AFTER TRUNCATE ON organization
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_truncate();
