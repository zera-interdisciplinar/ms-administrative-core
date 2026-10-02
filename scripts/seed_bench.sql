-- scripts/seed_bench.sql
--
-- Carga sintetica para medir plano de consulta. NAO e seed de desenvolvimento: gera volume
-- (centenas de milhares de linhas) para que o EXPLAIN ANALYZE mostre o plano que o Postgres
-- escolheria em producao.
--
-- POR QUE ISSO E NECESSARIO: com 50 linhas na tabela o planner sempre escolhe Seq Scan, porque
-- ler a tabela inteira e mais barato que abrir um indice. Medir otimizacao em banco vazio nao
-- mede nada -- todo indice parece inutil.
--
-- Uso:
--   psql -U postgres -d zera -f scripts/seed_bench.sql
--   ANALYZE;   -- obrigatorio: sem estatisticas atualizadas o planner decide no escuro

BEGIN;

-- Organizacoes -------------------------------------------------------------------------------
INSERT INTO organization (id, name, cnpj, status, email, plan, created_at, updated_at)
SELECT gen_random_uuid(),
       'Organizacao Bench ' || g,
       LPAD((11222333000000 + g)::TEXT, 14, '0'),
       'ACTIVE',
       'org' || g || '@bench.local',
       CASE WHEN g % 2 = 0 THEN 'PREMIUM' ELSE 'BASIC' END,
       LOCALTIMESTAMP(0) - INTERVAL '2 years',
       LOCALTIMESTAMP(0)
FROM generate_series(1, 2) g;

-- Unidades: 20 por organizacao ---------------------------------------------------------------
INSERT INTO unit (id, name, organization_id, created_at, updated_at)
SELECT gen_random_uuid(),
       'Unidade ' || o.name || ' #' || g,
       o.id,
       LOCALTIMESTAMP(0) - INTERVAL '18 months',
       LOCALTIMESTAMP(0)
FROM organization o
CROSS JOIN generate_series(1, 20) g
WHERE o.name LIKE 'Organizacao Bench%';

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
WHERE u.name LIKE 'Unidade Organizacao Bench%';

-- Gestores: um por unidade (raiz da hierarquia) ----------------------------------------------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Gestor ' || u.name,
       'MANAGER',
       RPAD('$2a$10$benchbenchbenchbenchbe', 60, 'x'),
       'gestor.' || replace(u.id::TEXT, '-', '') || '@bench.local',
       'ACTIVE',
       u.id,
       NULL,
       LOCALTIMESTAMP(0) - INTERVAL '18 months',
       LOCALTIMESTAMP(0)
FROM unit u
WHERE u.name LIKE 'Unidade Organizacao Bench%';

-- Gestor intermediario: nivel 2 da hierarquia, para a CTE recursiva ter o que recorrer ---------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Coordenador ' || m.name,
       'MANAGER',
       RPAD('$2a$10$benchbenchbenchbenchbe', 60, 'x'),
       'coord.' || replace(m.id::TEXT, '-', '') || '@bench.local',
       'ACTIVE',
       m.unit_id,
       m.id,
       LOCALTIMESTAMP(0) - INTERVAL '17 months',
       LOCALTIMESTAMP(0)
FROM user_account m
WHERE m.role = 'MANAGER' AND m.manager_id IS NULL AND m.name LIKE 'Gestor Unidade Organizacao Bench%';

-- Funcionarios: 15 por coordenador (nivel 3) --------------------------------------------------
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Funcionario ' || g || ' de ' || c.name,
       'EMPLOYEE',
       RPAD('$2a$10$benchbenchbenchbenchbe', 60, 'x'),
       'func' || g || '.' || replace(c.id::TEXT, '-', '') || '@bench.local',
       CASE WHEN g % 10 = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       c.unit_id,
       c.id,
       LOCALTIMESTAMP(0) - INTERVAL '16 months',
       LOCALTIMESTAMP(0)
FROM user_account c
CROSS JOIN generate_series(1, 15) g
WHERE c.name LIKE 'Coordenador Gestor Unidade Organizacao Bench%';

-- Escala de usuarios: +20 mil contas distribuidas nas mesmas unidades.
--
-- Nao e enfeite: com 680 usuarios o planner escolhe Seq Scan para QUALQUER filtro em
-- user_account, porque ler 680 linhas custa menos que abrir indice. Medir o ganho de um indice
-- em (unit_id, role) exigiria concluir "indice nao serve" a partir de um banco que nao existe.
-- Estes 20 mil colocam a tabela na ordem de grandeza em que a decisao do planner muda.
INSERT INTO user_account (id, name, role, password, email, status, unit_id, manager_id,
                          created_at, updated_at)
SELECT gen_random_uuid(),
       'Escala ' || g,
       CASE WHEN g % 500 = 0 THEN 'MANAGER' ELSE 'EMPLOYEE' END,
       RPAD('$2a$10$benchbenchbenchbenchbe', 60, 'x'),
       'escala' || g || '@bench.local',
       'ACTIVE',
       (SELECT id FROM unit ORDER BY id OFFSET (g % 40) LIMIT 1),
       NULL,
       LOCALTIMESTAMP(0) - INTERVAL '12 months',
       LOCALTIMESTAMP(0)
FROM generate_series(1, 20000) g;

-- Alertas: ~200 mil ao longo de 18 meses ------------------------------------------------------
-- A distribuicao e desigual de proposito (algumas unidades com muito mais alerta que outras),
-- senao o ranking e os quartis nao teriam nada para diferenciar.
INSERT INTO alert (id, status, unit_id, event_id, rule_id, user_id, description, notes,
                   severity, kind, occurred_at, created_at, updated_at)
SELECT gen_random_uuid(),
       CASE WHEN g % 4 = 0 THEN 'OPEN' ELSE 'CLOSED' END,
       u.unit_id,
       NULL,
       NULL,
       u.id,
       'Alerta sintetico ' || g,
       NULL,
       (ARRAY['LOW','LOW','LOW','MEDIUM','MEDIUM','HIGH'])[1 + (g % 6)],
       (ARRAY['TEMPERATURE_OUT_OF_RANGE','DISPOSAL_OVERDUE','CAPACITY_EXCEEDED','SENSOR_OFFLINE'])[1 + (g % 4)],
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL - ((g % 24) || ' hours')::INTERVAL,
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL,
       LOCALTIMESTAMP(0) - (((g * 7) % 540) || ' days')::INTERVAL + ((g % 72) || ' hours')::INTERVAL
FROM user_account u
CROSS JOIN generate_series(1, 400) g
WHERE u.role = 'EMPLOYEE' AND u.name LIKE 'Funcionario %de Coordenador%'
  -- Assimetria por unidade: cada unidade recebe entre ~50 e ~400 alertas por funcionario. Sem
  -- isso todas as unidades ficam com o mesmo total, e ranking, NTILE e percentil nao tem nada
  -- para diferenciar -- o teste passaria sem provar que a window function ordena de fato.
  AND g <= 50 + (abs(hashtext(u.unit_id::TEXT)) % 350);

COMMIT;

-- Acessos: inseridos via refresh_token para EXERCITAR O TRIGGER de DAU (V11), nao por INSERT
-- direto em user_access_log. Inserir direto no log deixaria o rollup vazio e daria a falsa
-- impressao de que a automacao funciona.
INSERT INTO refresh_token (id, user_id, token_hash, expires_at, revoked, created_at)
SELECT gen_random_uuid(),
       u.id,
       LPAD(md5(u.id::TEXT || g::TEXT), 64, '0'),
       LOCALTIMESTAMP(0) + INTERVAL '7 days',
       (g % 5 = 0),
       LOCALTIMESTAMP(0) - (((g * 2) % 120) || ' days')::INTERVAL - ((g % 17) || ' hours')::INTERVAL
FROM user_account u
CROSS JOIN generate_series(1, 60) g
WHERE u.name LIKE 'Funcionario %' OR u.name LIKE 'Coordenador %';

ANALYZE;
