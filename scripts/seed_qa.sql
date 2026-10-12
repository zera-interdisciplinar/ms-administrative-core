-- scripts/seed_qa.sql
--
-- Variante do seed_bench.sql para rodar a cada deploy em QA (ver .github/workflows/deploy-qa.yml).
--
-- POR QUE NAO REUSAR O seed_bench.sql DIRETO: aquele script existe para medir plano de consulta
-- (EXPLAIN ANALYZE) com centenas de milhares de linhas, de proposito -- e ali e correto. Rodar o
-- MESMO volume a cada push na branch qa deixaria o deploy lento so para gerar dado que ninguem
-- vai usar para benchmark naquele ambiente. Esta copia tem a mesma estrutura, em escala menor
-- (~5 mil linhas no total, nao ~250 mil), compativel com os limites de CPU/memoria do Postgres de
-- QA (50m/500m, 256Mi/512Mi -- ver k8s/postgres-qa.yaml).
--
-- IDEMPOTENTE: deploy em qa roda a cada push na branch. Sem a guarda abaixo, cada deploy duplicaria
-- todo o volume. A guarda verifica se a organizacao sintetica da PRIMEIRA execucao ja existe e, se
-- existir, sai sem fazer nada.
SELECT EXISTS (
    SELECT 1 FROM organization WHERE name = 'Organizacao QA 1'
) AS already_seeded \gset

\if :already_seeded
\echo 'seed_qa: QA ja tem o volume sintetico; pulando.'
\q
\endif

BEGIN;

-- Organizacoes -------------------------------------------------------------------------------
INSERT INTO organization (id, name, cnpj, status, email, plan, created_at, updated_at)
SELECT gen_random_uuid(),
       'Organizacao QA ' || g,
       -- CNPJs com digitos verificadores validos: a V19 aplica fn_validar_cnpj como CHECK.
       CASE g WHEN 1 THEN '11222333000181' WHEN 2 THEN '11444777000161' END,
       'ACTIVE',
       'org' || g || '@qa-seed.local',
       CASE WHEN g % 2 = 0 THEN 'PREMIUM' ELSE 'BASIC' END,
       LOCALTIMESTAMP(0) - INTERVAL '2 years',
       LOCALTIMESTAMP(0)
FROM generate_series(1, 2) g;

-- Unidades: 6 por organizacao -----------------------------------------------------------------
INSERT INTO unit (id, name, organization_id, created_at, updated_at)
SELECT gen_random_uuid(),
       'Unidade ' || o.name || ' #' || g,
       o.id,
       LOCALTIMESTAMP(0) - INTERVAL '18 months',
       LOCALTIMESTAMP(0)
FROM organization o
CROSS JOIN generate_series(1, 6) g
WHERE o.name LIKE 'Organizacao QA%';

INSERT INTO address (id, city, state, neighborhood, cep, number, unit_id, created_at, updated_at)
SELECT gen_random_uuid(),
       (ARRAY['Sao Paulo','Campinas','Santos','Sorocaba','Ribeirao Preto'])[1 + (row_number() OVER () % 5)],
       'SP',
       'Centro',
       '01310100',
       (100 + row_number() OVER ())::TEXT,
       u.id,
       LOCALTIMESTAMP(0) - INTERVAL '18 months',
       LOCALTIMESTAMP(0)
FROM unit u
WHERE u.name LIKE 'Unidade Organizacao QA%';

-- Gestores: um por unidade (raiz da hierarquia) ----------------------------------------------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Gestor ' || u.name,
       'MANAGER',
       RPAD('$2a$10$qaseedqaseedqaseedqas', 60, 'x'),
       'gestor.' || replace(u.id::TEXT, '-', '') || '@qa-seed.local',
       'ACTIVE',
       u.id,
       NULL,
       LOCALTIMESTAMP(0) - INTERVAL '18 months',
       LOCALTIMESTAMP(0)
FROM unit u
WHERE u.name LIKE 'Unidade Organizacao QA%';

-- Gestor intermediario: nivel 2 da hierarquia, para a CTE recursiva ter o que recorrer ---------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Coordenador ' || m.name,
       'MANAGER',
       RPAD('$2a$10$qaseedqaseedqaseedqas', 60, 'x'),
       'coord.' || replace(m.id::TEXT, '-', '') || '@qa-seed.local',
       'ACTIVE',
       m.unit_id,
       m.id,
       LOCALTIMESTAMP(0) - INTERVAL '17 months',
       LOCALTIMESTAMP(0)
FROM user_account m
WHERE m.role = 'MANAGER' AND m.manager_id IS NULL AND m.name LIKE 'Gestor Unidade Organizacao QA%';

-- Funcionarios: 5 por coordenador (nivel 3) ----------------------------------------------------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Funcionario ' || g || ' de ' || c.name,
       'EMPLOYEE',
       RPAD('$2a$10$qaseedqaseedqaseedqas', 60, 'x'),
       'func' || g || '.' || replace(c.id::TEXT, '-', '') || '@qa-seed.local',
       CASE WHEN g % 10 = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       c.unit_id,
       c.id,
       LOCALTIMESTAMP(0) - INTERVAL '16 months',
       LOCALTIMESTAMP(0)
FROM user_account c
CROSS JOIN generate_series(1, 5) g
WHERE c.name LIKE 'Coordenador Gestor Unidade Organizacao QA%';

-- Escala de usuarios: +1500 contas distribuidas nas mesmas unidades (reduzido de 20 mil).
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Escala ' || g,
       CASE WHEN g % 500 = 0 THEN 'MANAGER' ELSE 'EMPLOYEE' END,
       RPAD('$2a$10$qaseedqaseedqaseedqas', 60, 'x'),
       'escala' || g || '@qa-seed.local',
       'ACTIVE',
       (SELECT id FROM unit ORDER BY id OFFSET (g % 12) LIMIT 1),
       NULL,
       LOCALTIMESTAMP(0) - INTERVAL '12 months',
       LOCALTIMESTAMP(0)
FROM generate_series(1, 1500) g;

-- Alertas: assimetria por unidade (reduzido de ~400/func para ~80/func) -----------------------
INSERT INTO alert (id, status, unit_id, event_id, rule_id, user_id, description, notes,
                   severity, kind, occurred_at, created_at, updated_at)
SELECT gen_random_uuid(),
       CASE WHEN g % 4 = 0 THEN 'OPEN' ELSE 'CLOSED' END,
       u.unit_id,
       NULL,
       NULL,
       u.id,
       'Alerta sintetico QA ' || g,
       NULL,
       (ARRAY['LOW','LOW','LOW','MEDIUM','MEDIUM','HIGH'])[1 + (g % 6)],
       (ARRAY['TEMPERATURE_OUT_OF_RANGE','DISPOSAL_OVERDUE','CAPACITY_EXCEEDED','SENSOR_OFFLINE'])[1 + (g % 4)],
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL - ((g % 24) || ' hours')::INTERVAL,
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL,
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL + ((g % 72) || ' hours')::INTERVAL
FROM user_account u
CROSS JOIN generate_series(1, 80) g
WHERE u.role = 'EMPLOYEE' AND u.name LIKE 'Funcionario %de Coordenador%'
  AND g <= 10 + (abs(hashtext(u.unit_id::TEXT)) % 70);

COMMIT;

-- Acessos: inseridos via refresh_token para EXERCITAR O TRIGGER de DAU (V12), nao por INSERT
-- direto em user_access_log (reduzido de 60 para 20 por usuario).
INSERT INTO refresh_token (id, user_id, token_hash, expires_at, revoked, created_at)
SELECT gen_random_uuid(),
       u.id,
       LPAD(md5(u.id::TEXT || g::TEXT), 64, '0'),
       LOCALTIMESTAMP(0) + INTERVAL '7 days',
       (g % 5 = 0),
       LOCALTIMESTAMP(0) - (((g * 2) % 120) || ' days')::INTERVAL - ((g % 17) || ' hours')::INTERVAL
FROM user_account u
CROSS JOIN generate_series(1, 20) g
WHERE u.name LIKE 'Funcionario %' OR u.name LIKE 'Coordenador %';

ANALYZE;
