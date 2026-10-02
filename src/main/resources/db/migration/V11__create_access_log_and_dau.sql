-- V11__create_access_log_and_dau.sql
--
-- Registro de acesso e consolidacao de DAU (Daily Active User) dentro do proprio Postgres.
--
-- POR QUE UM TRIGGER EM refresh_token, e nao uma chamada na aplicacao: todo login e todo refresh
-- de sessao gravam exatamente uma linha em `refresh_token` (tokens opacos, um por sessao emitida).
-- Entao a insercao nessa tabela JA E o evento de acesso. Pendurar o registro nela deixa a metrica
-- imune a esquecimento no codigo: nenhum caminho novo de autenticacao pode "esquecer" de contar o
-- acesso, porque nenhum caminho de autenticacao consegue emitir sessao sem gravar o token.
--
-- `usuario_ativo_diario` e um rollup, nao a fonte da verdade: `user_access_log` e a fonte, e o
-- rollup pode ser reconstruido a qualquer momento por sp_consolidar_dau (V13). A duplicidade
-- existe porque DAU e consulta de dashboard: COUNT(DISTINCT) em milhoes de linhas a cada abertura
-- de tela nao se sustenta.

CREATE TABLE user_access_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,
    origem      VARCHAR(20) NOT NULL,
    ocorrido_em TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT user_access_log_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT user_access_log_origem_check CHECK (origem IN ('LOGIN', 'REFRESH', 'BACKFILL'))
);

-- (ocorrido_em, user_id) serve a consulta de BI, que filtra janela de dias e agrupa por usuario.
CREATE INDEX idx_user_access_log_dia_usuario ON user_access_log (ocorrido_em, user_id);
-- (user_id, ocorrido_em) serve o trigger: ele pergunta "este usuario ja acessou HOJE?", e essa
-- pergunta comeca pelo usuario. Com o indice invertido o teste viraria varredura do dia inteiro.
CREATE INDEX idx_user_access_log_usuario_dia ON user_access_log (user_id, ocorrido_em);

CREATE TABLE usuario_ativo_diario (
    dia             DATE NOT NULL,
    usuarios_ativos INTEGER NOT NULL,
    acessos         INTEGER NOT NULL,
    atualizado_em   TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (dia)
);

COMMENT ON TABLE user_access_log IS 'Log de acessos (grao: um evento de autenticacao).';
COMMENT ON TABLE usuario_ativo_diario IS
    'Rollup diario de DAU derivado de user_access_log; reconstruivel por sp_consolidar_dau.';

CREATE OR REPLACE FUNCTION fn_registrar_acesso()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_dia              DATE;
    v_origem           VARCHAR(20);
    v_primeiro_do_dia  BOOLEAN;
BEGIN
    -- A aplicacao diferencia login de refresh setando zera.access_origin; sem isso assume LOGIN,
    -- que e o caminho que cria sessao nova.
    v_origem := UPPER(COALESCE(NULLIF(current_setting('zera.access_origin', true), ''), 'LOGIN'));
    IF v_origem NOT IN ('LOGIN', 'REFRESH', 'BACKFILL') THEN
        v_origem := 'LOGIN';
    END IF;

    v_dia := NEW.created_at::DATE;

    -- Avaliado ANTES do INSERT abaixo: se o usuario ainda nao tem acesso hoje, este acesso soma
    -- +1 em usuarios_ativos; se ja tem, soma +0 e apenas incrementa `acessos`.
    --
    -- POR QUE INCREMENTAL E NAO COUNT(DISTINCT) DO DIA: recontar o dia inteiro a cada acesso custa
    -- O(n) por acesso, ou seja O(n^2) no dia -- num dia de 10 mil acessos sao ~50 milhoes de
    -- leituras de linha so para manter um contador. O teste de existencia abaixo usa
    -- idx_user_access_log_usuario_dia e custa uma busca de indice.
    --
    -- O PRECO 1 -- CONTAGEM: sob dois acessos simultaneos do MESMO usuario no MESMO dia, ambos
    -- podem ver "primeiro do dia" e o contador fica 1 acima. E erro de metrica, nao de dado: a
    -- fonte da verdade e `user_access_log`, e sp_consolidar_dau (V13) reconcilia o periodo por
    -- COUNT(DISTINCT) quando exatidao importar.
    --
    -- O PRECO 2 -- CONTENCAO: o `ON CONFLICT (dia) DO UPDATE` abaixo trava a linha DO DIA CORRENTE
    -- ate o commit. Como e sempre a MESMA linha, todos os logins concorrentes do dia serializam
    -- nela. A secao critica e curta (a transacao do login so grava o token; o bcrypt ja aconteceu
    -- na aplicacao), entao no volume deste servico e irrelevante -- mas e um gargalo real no
    -- caminho de autenticacao e precisa estar escrito, nao descoberto sob carga.
    --
    -- Se um dia isso apertar, a saida e de uma linha: remover deste trigger o bloco do rollup,
    -- deixando so o INSERT em `user_access_log` (append-only, sem contencao), e passar a consolidar
    -- exclusivamente por sp_consolidar_dau no agendador. O custo seria DAU do dia corrente
    -- defasado ate a proxima consolidacao.
    SELECT NOT EXISTS (
        SELECT 1 FROM user_access_log l
        WHERE l.user_id = NEW.user_id
          AND l.ocorrido_em >= v_dia
          AND l.ocorrido_em <  v_dia + 1
    ) INTO v_primeiro_do_dia;

    INSERT INTO user_access_log (user_id, origem, ocorrido_em)
    VALUES (NEW.user_id, v_origem, NEW.created_at);

    INSERT INTO usuario_ativo_diario (dia, usuarios_ativos, acessos, atualizado_em)
    VALUES (v_dia, CASE WHEN v_primeiro_do_dia THEN 1 ELSE 0 END, 1, LOCALTIMESTAMP(0))
    ON CONFLICT (dia) DO UPDATE
        SET usuarios_ativos = usuario_ativo_diario.usuarios_ativos + EXCLUDED.usuarios_ativos,
            acessos         = usuario_ativo_diario.acessos + EXCLUDED.acessos,
            atualizado_em   = EXCLUDED.atualizado_em;

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION fn_registrar_acesso() IS
    'Trigger de DAU: transforma a emissao de refresh token em evento de acesso e reconsolida o dia.';

CREATE TRIGGER trg_registrar_acesso
    AFTER INSERT ON refresh_token
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_acesso();
