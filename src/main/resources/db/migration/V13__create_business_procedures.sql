-- V13__create_business_procedures.sql
--
-- Procedures de manutencao. Sao PROCEDURE e nao FUNCTION porque ESCREVEM e porque o volume
-- escrito e imprevisivel: procedure pode controlar transacao, function roda dentro da transacao
-- de quem chamou.
--
-- Todas sao IDEMPOTENTES: rodar duas vezes seguidas nao muda mais nada na segunda. Isso e o que
-- permite agendar sem medo e rodar de novo depois de uma falha no meio.
--
-- Todas registram execucao em `job_execucao`. Job que nao deixa rastro e job que ninguem sabe se
-- parou de rodar: o registro existe para a pergunta "quando foi a ultima vez que isso rodou?"
-- ter resposta em SQL.

CREATE TABLE job_execucao (
    id            BIGSERIAL PRIMARY KEY,
    job           VARCHAR(60) NOT NULL,
    iniciado_em   TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    concluido_em  TIMESTAMP(0) WITHOUT TIME ZONE NULL,
    afetados      INTEGER NULL,
    detalhe       TEXT NULL,
    sucesso       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_job_execucao_job_inicio ON job_execucao (job, iniciado_em DESC);

COMMENT ON TABLE job_execucao IS 'Trilha de execucao das procedures de manutencao (observabilidade).';

-- ---------------------------------------------------------------------------------------------
-- 1) Fecha alertas OPEN antigos.
--
-- REGRA DE NEGOCIO CRITICA, e por isso mesmo NAO agendada por padrao: `alert.status` e observado
-- pelo ms-inventory, e fechar alerta automaticamente e decisao de produto, nao de infraestrutura.
-- A procedure existe e e acionavel pela rota interna; ligar no scheduler exige decisao explicita
-- (zera.maintenance.close-stale-alerts.enabled).
--
-- Usa `occurred_at` e nao `created_at`: o que importa e a idade do EVENTO, nao a idade da linha.
-- Um alerta reprocessado dias depois nasceria "novo" pelo created_at e nunca seria fechado.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_fechar_alertas_obsoletos(p_dias INTEGER, INOUT p_afetados INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_inicio TIMESTAMP(0) WITHOUT TIME ZONE := LOCALTIMESTAMP(0);
    v_limite TIMESTAMP(0) WITHOUT TIME ZONE;
BEGIN
    IF p_dias IS NULL OR p_dias < 1 THEN
        RAISE EXCEPTION 'p_dias deve ser >= 1 (recebido: %)', p_dias
            USING ERRCODE = 'invalid_parameter_value';
    END IF;

    v_limite := LOCALTIMESTAMP(0) - (p_dias || ' days')::INTERVAL;

    UPDATE alert
       SET status     = 'CLOSED',
           updated_at = LOCALTIMESTAMP(0),
           notes      = COALESCE(notes || E'\n', '')
                        || format('Fechado automaticamente por inatividade (>%s dias) em %s.',
                                  p_dias, LOCALTIMESTAMP(0))
     WHERE status = 'OPEN'
       AND occurred_at < v_limite;

    GET DIAGNOSTICS p_afetados = ROW_COUNT;

    INSERT INTO job_execucao (job, iniciado_em, concluido_em, afetados, detalhe)
    VALUES ('sp_fechar_alertas_obsoletos', v_inicio, LOCALTIMESTAMP(0), p_afetados,
            format('limite=%s', v_limite));
END;
$$;

-- ---------------------------------------------------------------------------------------------
-- 2) Revoga e depois expurga refresh tokens vencidos.
--
-- Dois passos separados de proposito: REVOGAR e barato e invalida a sessao na hora; REMOVER apaga
-- prova. Entre um e outro fica a janela de retencao, que e o tempo em que ainda da para
-- investigar "de onde veio essa sessao". Remover na hora deixaria a investigacao sem dado.
--
-- Nao mexe em `invitation`: o enum InvitationStatus do dominio tem apenas PENDING e USED, e
-- gravar 'EXPIRED' aqui faria o Java estourar ao ler a linha. Expirar convite depende de decisao
-- de produto (ver README / pendencias).
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_revogar_tokens_expirados(
    p_dias_retencao INTEGER,
    INOUT p_revogados INTEGER,
    INOUT p_removidos INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_inicio TIMESTAMP(0) WITHOUT TIME ZONE := LOCALTIMESTAMP(0);
BEGIN
    IF p_dias_retencao IS NULL OR p_dias_retencao < 0 THEN
        RAISE EXCEPTION 'p_dias_retencao deve ser >= 0 (recebido: %)', p_dias_retencao
            USING ERRCODE = 'invalid_parameter_value';
    END IF;

    UPDATE refresh_token
       SET revoked = TRUE
     WHERE revoked = FALSE
       AND expires_at < LOCALTIMESTAMP(0);
    GET DIAGNOSTICS p_revogados = ROW_COUNT;

    DELETE FROM refresh_token
     WHERE revoked = TRUE
       AND expires_at < LOCALTIMESTAMP(0) - (p_dias_retencao || ' days')::INTERVAL;
    GET DIAGNOSTICS p_removidos = ROW_COUNT;

    INSERT INTO job_execucao (job, iniciado_em, concluido_em, afetados, detalhe)
    VALUES ('sp_revogar_tokens_expirados', v_inicio, LOCALTIMESTAMP(0),
            p_revogados + p_removidos,
            format('revogados=%s removidos=%s retencao_dias=%s',
                   p_revogados, p_removidos, p_dias_retencao));
END;
$$;

-- ---------------------------------------------------------------------------------------------
-- 3) Reconsolida o rollup de DAU a partir do log.
--
-- A rede de seguranca do trigger da V11: se o trigger for desabilitado, se alguem carregar acesso
-- em massa com `ALTER TABLE ... DISABLE TRIGGER`, ou se o rollup for corrompido, esta procedure
-- reconstroi o periodo inteiro a partir de `user_access_log`, que e a fonte da verdade.
--
-- Escreve so os dias que TEM acesso: dia sem acesso nao vira linha zero, quem completa o
-- calendario e a bi.dim_tempo com LEFT JOIN. Gravar zeros criaria linha para todo dia da historia.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_consolidar_dau(p_de DATE, p_ate DATE, INOUT p_dias INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_inicio TIMESTAMP(0) WITHOUT TIME ZONE := LOCALTIMESTAMP(0);
BEGIN
    IF p_de IS NULL OR p_ate IS NULL OR p_ate < p_de THEN
        RAISE EXCEPTION 'Periodo invalido: de=% ate=%', p_de, p_ate
            USING ERRCODE = 'invalid_parameter_value';
    END IF;

    WITH agregado AS (
        SELECT l.ocorrido_em::DATE            AS dia,
               COUNT(DISTINCT l.user_id)::INT AS usuarios_ativos,
               COUNT(*)::INT                  AS acessos
        FROM user_access_log l
        WHERE l.ocorrido_em >= p_de
          AND l.ocorrido_em < p_ate + 1
        GROUP BY l.ocorrido_em::DATE
    )
    INSERT INTO usuario_ativo_diario (dia, usuarios_ativos, acessos, atualizado_em)
    SELECT dia, usuarios_ativos, acessos, LOCALTIMESTAMP(0) FROM agregado
    ON CONFLICT (dia) DO UPDATE
        SET usuarios_ativos = EXCLUDED.usuarios_ativos,
            acessos         = EXCLUDED.acessos,
            atualizado_em   = EXCLUDED.atualizado_em;

    GET DIAGNOSTICS p_dias = ROW_COUNT;

    INSERT INTO job_execucao (job, iniciado_em, concluido_em, afetados, detalhe)
    VALUES ('sp_consolidar_dau', v_inicio, LOCALTIMESTAMP(0), p_dias,
            format('de=%s ate=%s', p_de, p_ate));
END;
$$;
