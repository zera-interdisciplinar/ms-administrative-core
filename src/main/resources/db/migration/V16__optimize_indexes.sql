-- V16__optimize_indexes.sql
--
-- Indices e reescritas derivados de EXPLAIN (ANALYZE, BUFFERS) sobre a carga de
-- scripts/seed_bench.sql (~119 mil alertas, ~20,7 mil usuarios, ~241 mil linhas de auditoria).
-- Planos antes/depois em docs/otimizacao.md -- nenhum indice aqui entrou por intuicao.

-- ---------------------------------------------------------------------------------------------
-- 1) Alertas do usuario, paginado (GET /api/v1/notifications/alerts)
--
-- ANTES: Bitmap Index Scan em idx_alert_user_id -> Bitmap Heap Scan (filtrando status na heap)
--        -> top-N heapsort para o ORDER BY created_at DESC.
-- O indice sabia achar as linhas do usuario, mas nao sabia nada sobre status nem sobre ordem,
-- entao o Postgres pagava heap + sort em toda pagina.
--
-- O indice composto cobre os tres passos na ordem em que a consulta precisa: igualdade em
-- user_id, igualdade em status, e ordem ja pronta em created_at DESC -- o que elimina o Sort.
-- ---------------------------------------------------------------------------------------------
CREATE INDEX idx_alert_user_status_created ON alert (user_id, status, created_at DESC);

-- idx_alert_user_id (V3) virou PREFIXO EXATO do indice acima: qualquer consulta que o usava e
-- atendida pelo novo. Manter os dois custaria uma escrita a mais por INSERT/UPDATE de alerta sem
-- nenhum ganho de leitura -- indice redundante e custo puro no caminho de escrita.
DROP INDEX idx_alert_user_id;

-- ---------------------------------------------------------------------------------------------
-- 2) Alertas por unidade numa janela de tempo (views de BI)
-- ---------------------------------------------------------------------------------------------
CREATE INDEX idx_alert_unit_occurred ON alert (unit_id, occurred_at);

-- Mesmo argumento do DROP acima, aplicado ao par que faltava: `idx_alert_unit_id` (V3) e prefixo
-- exato de `idx_alert_unit_occurred`. Manter os dois custa uma escrita a mais por alerta gravado --
-- e alerta e justamente o que chega em rajada vindo do ms-inventory.
DROP INDEX idx_alert_unit_id;

-- ---------------------------------------------------------------------------------------------
-- 3) Usuarios por unidade (GET /api/v1/users?role=MANAGER&unitId=... -- contrato com o
--    ms-inventory, usado para achar o destinatario do alerta) e contagem de ativos por unidade.
--
-- ANTES: Seq Scan em user_account descartando 20.658 de 20.680 linhas para devolver 22. Com 680
-- linhas o Seq Scan era o plano certo; a partir de ~20 mil deixa de ser.
--
-- Dois indices e nao um: a consulta de destinatario filtra (unit_id, role) e a de saude da unidade
-- filtra (unit_id, status). `status` nao e prefixo de um indice que comeca em role, entao um unico
-- indice (unit_id, role, status) atenderia a primeira e nao a segunda.
-- ---------------------------------------------------------------------------------------------
CREATE INDEX idx_user_account_unit_role   ON user_account (unit_id, role);
CREATE INDEX idx_user_account_unit_status ON user_account (unit_id, status);

-- ---------------------------------------------------------------------------------------------
-- 4) Auditoria: exclusao por constraint nas filhas da heranca.
--
-- ANTES: `SELECT ... FROM audit_log WHERE tabela = 'alert'` fazia Parallel Append varrendo TODAS
-- as filhas, inclusive audit_log_user_account inteira (21.360 linhas descartadas pelo filtro).
-- A heranca ajuda a escrita, mas a leitura pelo pai nao ganha nada sozinha: o planner nao tem como
-- saber que `tabela` e constante dentro de cada filha.
--
-- O CHECK e o que informa isso. Com `constraint_exclusion = partition` (o padrao do Postgres para
-- heranca), o planner passa a PODAR as filhas cujo CHECK contradiz o WHERE, e a consulta encosta
-- so na filha certa. E o CHECK tambem protege a integridade: impede que a trigger, um dia,
-- escreva a linha de uma tabela na filha de outra.
-- ---------------------------------------------------------------------------------------------
ALTER TABLE audit_log_user_account
    ADD CONSTRAINT audit_log_user_account_tabela_check CHECK (tabela = 'user_account');
ALTER TABLE audit_log_alert
    ADD CONSTRAINT audit_log_alert_tabela_check CHECK (tabela = 'alert');
ALTER TABLE audit_log_organization
    ADD CONSTRAINT audit_log_organization_tabela_check CHECK (tabela = 'organization');

-- Ordem por data para a consulta "ultimas alteracoes" nao precisar de Sort.
CREATE INDEX idx_audit_user_account_data ON audit_log_user_account (ocorrido_em DESC);
CREATE INDEX idx_audit_alert_data        ON audit_log_alert        (ocorrido_em DESC);
CREATE INDEX idx_audit_organization_data ON audit_log_organization (ocorrido_em DESC);

-- ---------------------------------------------------------------------------------------------
-- 5) Reescrita da bi.vw_ranking_unidades: 1118 ms -> ver docs/otimizacao.md.
--
-- Dois problemas no plano original, e nenhum deles se resolve com indice:
--
-- (a) SUBCONSULTA CORRELACIONADA. `(SELECT COUNT(*) FROM user_account WHERE unit_id = p.unidade_id
--     AND status = 'ACTIVE')` executava uma vez POR UNIDADE (loops=40), cada uma varrendo as 20 mil
--     linhas de user_account. Virou um LEFT JOIN com agregacao unica: uma passada na tabela em vez
--     de quarenta.
--
-- (b) CAST ESCONDIDO NO FILTRO. O join usava `f.data_id >= CURRENT_DATE - 90`, e `data_id` e
--     `occurred_at::DATE` dentro de bi.fato_alerta. Filtrar por uma EXPRESSAO da coluna torna o
--     indice em occurred_at inutilizavel -- o Postgres teria de aplicar o cast em cada linha para
--     saber se ela entra, o que e a definicao de Seq Scan. Filtrando direto em `occurred_at` (que o
--     fato tambem expoe) o indice de (2) passa a ser usado. Este e o custo escondido de uma
--     dimensao de tempo: a coluna derivada e otima para AGRUPAR e pessima para FILTRAR.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE VIEW bi.vw_ranking_unidades AS
WITH periodo AS (
    SELECT d.unidade_id,
           d.unidade,
           d.organizacao_id,
           d.cidade,
           d.estado,
           COUNT(f.alerta_id)::INT                  AS total_alertas,
           COALESCE(SUM(f.peso_severidade), 0)::INT AS peso_total,
           COALESCE(SUM(f.aberto), 0)::INT          AS alertas_abertos
    FROM bi.dim_unidade d
    LEFT JOIN bi.fato_alerta f
           ON f.unidade_id = d.unidade_id
          AND f.occurred_at >= CURRENT_DATE - 90
    GROUP BY d.unidade_id, d.unidade, d.organizacao_id, d.cidade, d.estado
),
ativos_por_unidade AS (
    SELECT u.unit_id AS unidade_id, COUNT(*)::INT AS usuarios_ativos
    FROM user_account u
    WHERE u.status = 'ACTIVE'
    GROUP BY u.unit_id
),
com_usuarios AS (
    SELECT p.*, COALESCE(a.usuarios_ativos, 0) AS usuarios_ativos
    FROM periodo p
    LEFT JOIN ativos_por_unidade a ON a.unidade_id = p.unidade_id
)
SELECT c.*,
       ROUND(c.peso_total::NUMERIC / GREATEST(c.usuarios_ativos, 1), 2) AS peso_por_usuario,
       RANK()       OVER (ORDER BY c.peso_total DESC) AS ranking_geral,
       DENSE_RANK() OVER (PARTITION BY c.organizacao_id ORDER BY c.peso_total DESC)
           AS ranking_na_organizacao,
       NTILE(4)     OVER (ORDER BY c.peso_total DESC) AS quartil_gravidade,
       ROUND((PERCENT_RANK() OVER (ORDER BY c.peso_total))::NUMERIC * 100, 2) AS percentil,
       ROUND(100.0 * c.peso_total / NULLIF(SUM(c.peso_total) OVER (), 0), 2)
           AS participacao_percentual
FROM com_usuarios c;

-- Estatisticas atualizadas: um indice novo nao e usado enquanto o planner nao souber a cardinalidade
-- da coluna. Sem isto, a primeira consulta depois do deploy ainda roda no plano antigo.
ANALYZE alert;
ANALYZE user_account;
ANALYZE audit_log_alert;
ANALYZE audit_log_user_account;
ANALYZE audit_log_organization;
