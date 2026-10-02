-- V14__create_bi_star_schema.sql
--
-- Modelagem dimensional (Snowflake) para consumo por ferramenta de BI.
--
-- POR QUE VIEW E NAO TABELA FISICA: o volume do servico administrativo e pequeno (organizacoes,
-- unidades, usuarios) e o dado precisa estar atualizado no segundo. Materializar exigiria um
-- processo de refresh, uma janela de defasagem e um lugar para o refresh falhar. View resolve o
-- requisito de modelagem sem criar um pipeline para manter. Se o volume crescer, o caminho e
-- trocar `CREATE VIEW` por `CREATE MATERIALIZED VIEW` + refresh na sp_consolidar_dau -- a
-- interface de consulta nao muda.
--
-- POR QUE SNOWFLAKE E NAO STAR PURO: `dim_unidade` referencia `dim_organizacao` em vez de repetir
-- plano/status/cnpj da organizacao em cada unidade. A hierarquia organizacao->unidade ja e real no
-- OLTP, e desnormalizar aqui criaria duas fontes para o mesmo atributo.
--
-- Schema separado (`bi`) porque o nivel de acesso e diferente: a role zera_bi_leitor (V16) ve `bi`
-- e nao ve `public`. Ferramenta de BI nao precisa (e nao deve) alcancar hash de senha.
--
-- O QUE O BI *AINDA* ENXERGA, e por que: nome de pessoa, nome de unidade e cidade continuam aqui,
-- porque sem rotulo um painel nao rotula nada. Isso e dado pessoal, entao a ferramenta de BI esta
-- DENTRO do perimetro de LGPD -- nao fora dele. O que foi removido destas views por nao ter uso
-- analitico nenhum: CNPJ, e-mail e bairro. A regra usada: se o campo nao muda nenhum grafico, ele
-- nao entra.

CREATE SCHEMA bi;

COMMENT ON SCHEMA bi IS 'Camada analitica (modelagem dimensional) para ferramentas de BI. Somente leitura.';

-- =============================================================================================
-- DIMENSOES
-- =============================================================================================

-- CTE RECURSIVA gerando o calendario. Uma dimensao de tempo precisa conter TODO dia do periodo,
-- inclusive os dias sem fato -- e exatamente isso que permite um grafico mostrar "zero acessos na
-- terca" em vez de pular a terca. `generate_series` faria o mesmo, mas a recursao e o que o
-- requisito pede e deixa explicito o passo (dia + 1).
--
-- O INICIO E DIRIGIDO PELOS DADOS, e nao uma constante. Motivo concreto: as views analiticas fazem
-- JOIN com esta dimensao, e JOIN descarta o que nao casa. Com um inicio fixo em 2024-01-01, todo
-- alerta anterior a essa data SUMIA da bi.vw_alertas_mensal_unidade -- sem erro, sem aviso, so um
-- total menor. Foi verificado: o fato mostrava 2 alertas e a view mensal, 1. E o pior tipo de bug
-- de BI, porque o numero errado parece plausivel.
--
-- O piso de 2000-01-01 protege o outro lado: uma data absurda em `occurred_at` (typo de ano, dado
-- legado sujo) geraria milhoes de linhas de calendario a cada consulta.
CREATE VIEW bi.dim_tempo AS
WITH RECURSIVE limites AS (
    SELECT GREATEST(
               DATE '2000-01-01',
               LEAST(
                   DATE '2024-01-01',
                   COALESCE((SELECT MIN(occurred_at)::DATE FROM alert),      DATE '2024-01-01'),
                   COALESCE((SELECT MIN(created_at)::DATE FROM organization), DATE '2024-01-01')
               )
           ) AS inicio,
           (DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year' - INTERVAL '1 day')::DATE AS fim
),
calendario (dia, fim) AS (
    SELECT inicio, fim FROM limites
    UNION ALL
    SELECT dia + 1, fim
    FROM calendario
    WHERE dia < fim
)
SELECT dia                                             AS data_id,
       EXTRACT(YEAR    FROM dia)::INT                  AS ano,
       EXTRACT(QUARTER FROM dia)::INT                  AS trimestre,
       EXTRACT(MONTH   FROM dia)::INT                  AS mes,
       TO_CHAR(dia, 'YYYY-MM')                         AS ano_mes,
       DATE_TRUNC('month', dia)::DATE                  AS primeiro_dia_mes,
       EXTRACT(WEEK    FROM dia)::INT                  AS semana_iso,
       EXTRACT(DAY     FROM dia)::INT                  AS dia_do_mes,
       EXTRACT(ISODOW  FROM dia)::INT                  AS dia_da_semana,
       TO_CHAR(dia, 'TMDay')                           AS nome_dia_semana,
       EXTRACT(ISODOW FROM dia) IN (6, 7)              AS fim_de_semana
FROM calendario;

COMMENT ON VIEW bi.dim_tempo IS
    'Dimensao de tempo (grao: dia), gerada por CTE recursiva de 2024-01-01 ao fim do ano corrente.';

-- Sem `cnpj` e sem `email`: nenhum painel agrupa ou filtra por eles, e ambos sao dado pessoal
-- classificado como CONFIDENCIAL no catalogo (V17).
CREATE VIEW bi.dim_organizacao AS
SELECT o.id            AS organizacao_id,
       o.name          AS organizacao,
       o.plan          AS plano,
       o.status,
       o.created_at::DATE AS data_cadastro
FROM organization o;

-- DISTINCT ON e essencial: `address.unit_id` nao tem unicidade no schema, entao uma unidade PODE
-- ter mais de um endereco. Sem isso, a unidade apareceria duas vezes na dimensao e todo fato
-- ligado a ela seria contado em dobro no BI -- o classico fan-out de join.
CREATE VIEW bi.dim_unidade AS
SELECT u.id              AS unidade_id,
       u.name            AS unidade,
       u.organization_id AS organizacao_id,
       e.city            AS cidade,
       e.state           AS estado,
       u.created_at::DATE AS data_cadastro
FROM unit u
LEFT JOIN (
    -- Sem `neighborhood`: cidade e estado bastam para recorte geografico; bairro so aproxima do
    -- endereco da pessoa sem responder nenhuma pergunta analitica nova.
    SELECT DISTINCT ON (a.unit_id) a.unit_id, a.city, a.state
    FROM address a
    WHERE a.unit_id IS NOT NULL
    ORDER BY a.unit_id, a.created_at DESC
) e ON e.unit_id = u.id;

COMMENT ON VIEW bi.dim_unidade IS
    'Dimensao de unidade com o endereco mais recente; aponta para bi.dim_organizacao (Snowflake).';

CREATE VIEW bi.dim_usuario AS
SELECT u.id       AS usuario_id,
       u.name     AS usuario,
       u.role     AS papel,
       u.status,
       u.unit_id  AS unidade_id,
       u.manager_id AS gestor_id,
       g.name     AS gestor,
       u.created_at::DATE AS data_cadastro
FROM user_account u
LEFT JOIN user_account g ON g.id = u.manager_id;

-- =============================================================================================
-- FATOS
-- =============================================================================================

-- Grao: um alerta. `peso_severidade` e a medida aditiva que permite somar gravidade em vez de so
-- contar linhas -- 10 alertas LOW nao equivalem a 10 alertas HIGH, e um COUNT trataria igual.
CREATE VIEW bi.fato_alerta AS
SELECT a.id                  AS alerta_id,
       a.occurred_at::DATE   AS data_id,
       a.unit_id             AS unidade_id,
       a.user_id             AS usuario_id,
       a.kind                AS tipo,
       a.severity            AS severidade,
       a.status,
       CASE a.severity WHEN 'HIGH' THEN 9 WHEN 'MEDIUM' THEN 3 ELSE 1 END AS peso_severidade,
       1                     AS quantidade,
       CASE WHEN a.status = 'OPEN' THEN 1 ELSE 0 END AS aberto,
       -- GREATEST(0, ...): `occurred_at` e a hora do EVENTO e pode ser retro-datada num
       -- reprocessamento, ficando DEPOIS de updated_at. Sem o piso, a diferenca vira negativa e
       -- contamina a media de tempo de fechamento -- um numero que ninguem confere porque parece
       -- so "baixo".
       CASE WHEN a.status <> 'OPEN'
            THEN GREATEST(0, ROUND(EXTRACT(EPOCH FROM (a.updated_at - a.occurred_at)) / 3600.0, 2))
       END                   AS horas_ate_fechamento,
       a.occurred_at
FROM alert a;

COMMENT ON VIEW bi.fato_alerta IS 'Fato de alertas (grao: 1 alerta). Medidas: quantidade, peso_severidade, horas_ate_fechamento.';

-- Grao: um acesso. Traz `unidade_id` do usuario para permitir DAU por unidade sem a ferramenta de
-- BI ter de navegar duas dimensoes.
CREATE VIEW bi.fato_acesso AS
SELECT l.id               AS acesso_id,
       l.ocorrido_em::DATE AS data_id,
       l.user_id          AS usuario_id,
       u.unit_id          AS unidade_id,
       l.origem,
       1                  AS quantidade,
       l.ocorrido_em
FROM user_access_log l
JOIN user_account u ON u.id = l.user_id;

-- =============================================================================================
-- VIEWS ANALITICAS (ETL em SQL: CTEs organizam a transformacao, window functions calculam)
-- =============================================================================================

-- Running total, ranking e variacao mes a mes de alertas por unidade.
--
-- As tres window functions respondem perguntas que GROUP BY nao responde:
--   total_acumulado  -> "quanto essa unidade acumulou no ano ate este mes"
--   ranking_no_mes   -> "qual a posicao dela entre as unidades NAQUELE mes"
--   variacao_mom     -> "piorou ou melhorou em relacao ao mes anterior"
-- Todas precisam ver as OUTRAS linhas do resultado, o que so window function faz.
CREATE VIEW bi.vw_alertas_mensal_unidade AS
WITH base AS (
    -- Agrega no grao (mes, unidade). Esta CTE e a unica que toca o fato.
    SELECT t.primeiro_dia_mes             AS mes,
           t.ano_mes,
           d.unidade_id,
           d.unidade,
           d.organizacao_id,
           SUM(f.quantidade)::INT         AS total_alertas,
           SUM(f.peso_severidade)::INT    AS peso_total,
           SUM(f.aberto)::INT             AS alertas_abertos,
           ROUND(AVG(f.horas_ate_fechamento), 2) AS media_horas_fechamento
    FROM bi.fato_alerta f
    JOIN bi.dim_tempo   t ON t.data_id = f.data_id
    JOIN bi.dim_unidade d ON d.unidade_id = f.unidade_id
    GROUP BY t.primeiro_dia_mes, t.ano_mes, d.unidade_id, d.unidade, d.organizacao_id
),
janelas AS (
    -- Calculos analiticos sobre o resultado agregado.
    SELECT b.*,
           SUM(b.total_alertas) OVER (PARTITION BY b.unidade_id ORDER BY b.mes
                                      ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
               AS total_acumulado,
           SUM(b.peso_total) OVER (PARTITION BY b.unidade_id ORDER BY b.mes
                                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
               AS peso_acumulado,
           DENSE_RANK() OVER (PARTITION BY b.mes ORDER BY b.total_alertas DESC)
               AS ranking_no_mes,
           LAG(b.total_alertas) OVER (PARTITION BY b.unidade_id ORDER BY b.mes)
               AS total_mes_anterior,
           ROUND(AVG(b.total_alertas) OVER (PARTITION BY b.unidade_id ORDER BY b.mes
                                            ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 2)
               AS media_movel_3m
    FROM base b
)
SELECT j.*,
       j.total_alertas - COALESCE(j.total_mes_anterior, 0) AS variacao_mom,
       CASE
           WHEN j.total_mes_anterior IS NULL OR j.total_mes_anterior = 0 THEN NULL
           ELSE ROUND(100.0 * (j.total_alertas - j.total_mes_anterior) / j.total_mes_anterior, 2)
       END AS variacao_mom_percentual
FROM janelas j;

COMMENT ON VIEW bi.vw_alertas_mensal_unidade IS
    'Alertas por mes/unidade com running total, DENSE_RANK mensal, media movel 3m e variacao MoM.';

-- DAU com media movel de 7 dias e usuarios unicos na janela de 7 dias (WAU movel).
--
-- O LEFT JOIN a partir de dim_tempo e o que faz o dia sem acesso aparecer com zero. Se o FROM
-- fosse o rollup, o grafico simplesmente omitiria o dia, e uma queda a zero ficaria invisivel.
--
-- `usuarios_unicos_7d` usa LATERAL e nao window function porque COUNT(DISTINCT) nao e suportado
-- como funcao de janela no Postgres -- e uma limitacao real, nao uma escolha de estilo.
CREATE VIEW bi.vw_dau_diario AS
WITH dias AS (
    SELECT t.data_id,
           t.ano_mes,
           t.fim_de_semana,
           COALESCE(d.usuarios_ativos, 0) AS dau,
           COALESCE(d.acessos, 0)         AS acessos
    FROM bi.dim_tempo t
    LEFT JOIN usuario_ativo_diario d ON d.dia = t.data_id
    WHERE t.data_id <= CURRENT_DATE
),
janelas AS (
    SELECT d.*,
           ROUND(AVG(d.dau) OVER (ORDER BY d.data_id ROWS BETWEEN 6 PRECEDING AND CURRENT ROW), 2)
               AS dau_media_movel_7d,
           SUM(d.acessos) OVER (ORDER BY d.data_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
               AS acessos_acumulados,
           LAG(d.dau) OVER (ORDER BY d.data_id) AS dau_dia_anterior,
           RANK() OVER (PARTITION BY d.ano_mes ORDER BY d.dau DESC) AS ranking_no_mes
    FROM dias d
)
SELECT j.data_id,
       j.ano_mes,
       j.fim_de_semana,
       j.dau,
       j.acessos,
       j.dau_media_movel_7d,
       j.acessos_acumulados,
       j.dau - COALESCE(j.dau_dia_anterior, 0) AS variacao_dod,
       j.ranking_no_mes,
       w.usuarios_unicos_7d,
       CASE WHEN w.usuarios_unicos_7d = 0 THEN NULL
            ELSE ROUND(100.0 * j.dau / w.usuarios_unicos_7d, 2)
       END AS aderencia_dau_wau
FROM janelas j
LEFT JOIN LATERAL (
    SELECT COUNT(DISTINCT l.user_id)::INT AS usuarios_unicos_7d
    FROM user_access_log l
    WHERE l.ocorrido_em >= j.data_id - 6
      AND l.ocorrido_em <  j.data_id + 1
) w ON TRUE;

COMMENT ON VIEW bi.vw_dau_diario IS
    'DAU diario com media movel 7d, acumulado, variacao DoD, ranking no mes e aderencia DAU/WAU.';

-- Ranking de unidades no periodo de 90 dias, com quartis.
--
-- NTILE(4) divide as unidades em quartis de gravidade: e o corte que responde "quais 25% estao
-- pior" sem escolher um limiar arbitrario de alertas na mao.
CREATE VIEW bi.vw_ranking_unidades AS
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
          AND f.data_id >= CURRENT_DATE - 90
    GROUP BY d.unidade_id, d.unidade, d.organizacao_id, d.cidade, d.estado
),
com_usuarios AS (
    SELECT p.*,
           (SELECT COUNT(*)::INT FROM user_account u
             WHERE u.unit_id = p.unidade_id AND u.status = 'ACTIVE') AS usuarios_ativos
    FROM periodo p
)
SELECT c.*,
       ROUND(c.peso_total::NUMERIC / GREATEST(c.usuarios_ativos, 1), 2) AS peso_por_usuario,
       RANK()         OVER (ORDER BY c.peso_total DESC)  AS ranking_geral,
       DENSE_RANK()   OVER (PARTITION BY c.organizacao_id ORDER BY c.peso_total DESC)
           AS ranking_na_organizacao,
       NTILE(4)       OVER (ORDER BY c.peso_total DESC)  AS quartil_gravidade,
       ROUND((PERCENT_RANK() OVER (ORDER BY c.peso_total))::NUMERIC * 100, 2) AS percentil,
       ROUND(100.0 * c.peso_total / NULLIF(SUM(c.peso_total) OVER (), 0), 2)
           AS participacao_percentual
FROM com_usuarios c;

COMMENT ON VIEW bi.vw_ranking_unidades IS
    'Ranking de unidades por gravidade nos ultimos 90 dias, com quartil (NTILE), percentil e participacao.';

-- Hierarquia de equipes por CTE RECURSIVA.
--
-- Responde "quem esta sob quem, e em que nivel" -- algo que exige numero VARIAVEL de joins e por
-- isso e impossivel em SQL nao recursivo. `caminho` protege contra ciclo em manager_id, que o
-- schema permite fisicamente.
CREATE VIEW bi.vw_hierarquia_equipe AS
WITH RECURSIVE arvore AS (
    -- Raizes: quem nao tem gestor.
    SELECT u.id                AS usuario_id,
           u.name              AS usuario,
           u.role              AS papel,
           u.manager_id        AS gestor_id,
           u.unit_id           AS unidade_id,
           1                   AS nivel,
           u.name::TEXT        AS caminho_texto,
           ARRAY[u.id]         AS caminho,
           u.id                AS raiz_id
    FROM user_account u
    WHERE u.manager_id IS NULL

    UNION ALL

    SELECT u.id,
           u.name,
           u.role,
           u.manager_id,
           u.unit_id,
           a.nivel + 1,
           a.caminho_texto || ' > ' || u.name,
           a.caminho || u.id,
           a.raiz_id
    FROM user_account u
    JOIN arvore a ON u.manager_id = a.usuario_id
    WHERE NOT u.id = ANY (a.caminho)
)
,
-- POR QUE NAO fn_tamanho_equipe(a.usuario_id) AQUI: aquela funcao e escalar e correlacionada, e
-- seria executada UMA VEZ POR LINHA -- cada execucao relancando a descida recursiva inteira. Medido
-- na carga de bench (20.679 usuarios): a view levava 14.181 ms, dos quais ~13 s eram so as 20 mil
-- reexecucoes da funcao; a arvore sozinha custa 1.330 ms. E o classico N+1, escondido atras de algo
-- que parece uma coluna calculada inocente.
--
-- A contagem abaixo sai em UMA passada: o `caminho` de cada no ja contem todos os seus ancestrais,
-- entao quantas vezes X aparece nos caminhos dos outros nos E, exatamente, o numero de descendentes
-- de X.
descendentes AS (
    SELECT ancestral, COUNT(*)::INT AS total
    FROM arvore a
    CROSS JOIN LATERAL unnest(a.caminho) AS ancestral
    WHERE ancestral <> a.usuario_id
    GROUP BY ancestral
)
SELECT a.usuario_id,
       a.usuario,
       a.papel,
       a.gestor_id,
       a.unidade_id,
       a.nivel,
       a.caminho_texto,
       a.raiz_id,
       COALESCE(d.total, 0) AS tamanho_equipe
FROM arvore a
LEFT JOIN descendentes d ON d.ancestral = a.usuario_id;

COMMENT ON VIEW bi.vw_hierarquia_equipe IS
    'Hierarquia de gestores/subordinados por CTE recursiva, com nivel, caminho e tamanho da equipe.';
