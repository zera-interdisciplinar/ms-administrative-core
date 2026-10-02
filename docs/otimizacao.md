# Otimizacao de consultas

Medicoes reais, com `EXPLAIN (ANALYZE, BUFFERS)`, em Postgres 16 (Alpine, container local) sobre a
carga de [`scripts/seed_bench.sql`](../scripts/seed_bench.sql):

| Tabela | Linhas |
| --- | --- |
| `alert` | 118.740 |
| `user_account` | 20.680 |
| `audit_log` (todas as filhas) | ~241.000 |
| `user_access_log` | 38.400 |

> **Por que o volume importa.** Com 50 linhas o planner escolhe `Seq Scan` para qualquer filtro,
> porque ler a tabela inteira custa menos que abrir um indice. Medir otimizacao em banco vazio nao
> mede nada: todo indice parece inutil. O `seed_bench.sql` existe para colocar as tabelas na ordem de
> grandeza em que a decisao do planner muda.

## Resumo

| # | Consulta | Antes | Depois | Ganho |
| --- | --- | --- | --- | --- |
| Q1 | Alertas do usuario, paginado (`GET /notifications/alerts`) | 1,786 ms | 0,159 ms | **11x** |
| Q2 | Gestores da unidade (`GET /users?role=MANAGER&unitId=`) | 4,978 ms | 0,187 ms | **27x** |
| Q3 | View mensal de BI para uma unidade | 165,8 ms | 163,0 ms | — (ver Q3) |
| Q4 | Ranking de unidades (90 dias) | 980,1 ms | 33,2 ms | **30x** |
| Q5 | Auditoria recente de uma tabela | 48,9 ms | 0,103 ms | **475x** |

O "antes" foi medido revertendo a V15 (indices removidos, view antiga restaurada) **sobre a mesma
carga**, nao num banco menor. Sem isso a comparacao mediria volume, nao otimizacao.

---

## Q1 — Alertas do usuario, paginado

```sql
SELECT * FROM alert WHERE user_id = ? AND status = 'CLOSED'
ORDER BY created_at DESC LIMIT 20;
```

**Antes** (1,786 ms):

```
Limit
  -> Sort  (Sort Key: alert.created_at DESC, top-N heapsort  Memory: 30kB)
       -> Bitmap Heap Scan on alert  (rows=123)
            Filter: ((status)::text = 'CLOSED'::text)
            Rows Removed by Filter: 40
            Heap Blocks: exact=163
            -> Bitmap Index Scan on idx_alert_user_id  (rows=163)
```

Tres custos, todos visiveis no plano:

1. `idx_alert_user_id` sabia achar as linhas do usuario, mas nada sobre `status` — o filtro sobrava
   para a heap (`Rows Removed by Filter: 40`).
2. `Heap Blocks: exact=163` — 163 blocos lidos para devolver 20 linhas.
3. `Sort` com `top-N heapsort`: o indice nao tinha ordem util, entao ordenar era inevitavel.

**Depois** (0,159 ms):

```
Limit
  -> Index Scan using idx_alert_user_status_created on alert  (rows=20)
```

```sql
CREATE INDEX idx_alert_user_status_created ON alert (user_id, status, created_at DESC);
DROP INDEX idx_alert_user_id;  -- prefixo exato do novo: redundante
```

O indice cobre os tres passos na ordem em que a consulta precisa: igualdade em `user_id`, igualdade
em `status`, ordem pronta em `created_at DESC`. O `Sort` **desaparece do plano** — nao ficou mais
rapido, deixou de existir.

O `DROP INDEX` e parte da otimizacao, nao limpeza: `idx_alert_user_id` virou prefixo exato do novo
indice, entao toda consulta que o usava e atendida pelo composto. Mantido, custaria uma escrita a
mais por `INSERT`/`UPDATE` de alerta — custo puro no caminho de escrita, que e o caminho quente
(alertas chegam do `ms-inventory` em rajada).

---

## Q2 — Gestores da unidade

```sql
SELECT * FROM user_account WHERE unit_id = ? AND role = 'MANAGER';
```

**Antes** (4,978 ms):

```
Seq Scan on user_account
  Filter: ((unit_id = $0) AND ((role)::text = 'MANAGER'::text))
  Rows Removed by Filter: 20658
  Buffers: shared hit=521
```

20.658 linhas descartadas para devolver 22. Detalhe importante: **com 680 usuarios este plano estava
certo.** O `Seq Scan` so deixou de ser o melhor plano quando a tabela passou de ~20 mil linhas. Um
indice criado cedo demais teria sido custo de escrita sem retorno.

**Depois** (0,187 ms):

```
Bitmap Heap Scan on user_account  (rows=22)
  -> Bitmap Index Scan on idx_user_account_unit_role  (rows=22)
```

```sql
CREATE INDEX idx_user_account_unit_role   ON user_account (unit_id, role);
CREATE INDEX idx_user_account_unit_status ON user_account (unit_id, status);
```

Dois indices e nao um: esta consulta filtra `(unit_id, role)` e a contagem de ativos por unidade
filtra `(unit_id, status)`. `status` nao e prefixo de um indice que comeca em `role`, entao um unico
`(unit_id, role, status)` atenderia a primeira e nao a segunda.

Esta consulta e **contrato com o `ms-inventory`** (e por ela que se acha o destinatario de um
alerta), o que a torna quente por definicao.

---

## Q3 — View mensal de BI: o caso em que indice nao resolve

```sql
SELECT * FROM bi.vw_alertas_mensal_unidade WHERE unidade_id = ?;
```

165,8 ms antes, 163,0 ms depois. **Nenhum ganho, e esta correto que nao haja.** O plano mostra por
que:

```
-> Seq Scan on alert a  (rows=118740)
```

A view calcula `DENSE_RANK() OVER (PARTITION BY mes ORDER BY total_alertas DESC)`. Para saber a
posicao de **uma** unidade num mes, o banco precisa dos totais de **todas** as unidades naquele mes.
O filtro `WHERE unidade_id = ?` nao pode ser empurrado para dentro da CTE sem mudar o resultado do
ranking — o Postgres nao empurra, e faz certo.

Ou seja: a leitura completa de `alert` e **inerente a pergunta**, nao um defeito de plano. As saidas
reais sao outras:

- aceitar 160 ms numa consulta de painel (escolha atual, e a certa por enquanto);
- trocar `CREATE VIEW` por `CREATE MATERIALIZED VIEW` + `REFRESH` na `sp_consolidar_dau`, quando o
  volume justificar — a interface de consulta nao muda;
- calcular o ranking numa janela menor (ex.: 12 meses) se o historico completo deixar de ser util.

Registrar um "otimizamos e nao melhorou" vale mais que esconder o caso: sem esta secao, a proxima
pessoa gasta um dia criando indices em `alert` para esta consulta.

---

## Q4 — Ranking de unidades: o pior plano, e nenhum dos dois problemas era indice

980,1 ms → 33,2 ms (**30x**). Duas causas independentes:

### (a) Subconsulta correlacionada

```sql
-- antes
(SELECT COUNT(*) FROM user_account u
  WHERE u.unit_id = p.unidade_id AND u.status = 'ACTIVE') AS usuarios_ativos
```

No plano:

```
-> Seq Scan on user_account u_1  (actual time=0.003..1.176 rows=516 loops=40)
   Rows Removed by Filter: 20164
```

`loops=40`: a subconsulta executava **uma vez por unidade**, cada uma varrendo as 20 mil linhas de
`user_account`. Reescrita como agregacao unica + `LEFT JOIN`:

```sql
ativos_por_unidade AS (
    SELECT u.unit_id AS unidade_id, COUNT(*)::INT AS usuarios_ativos
    FROM user_account u WHERE u.status = 'ACTIVE' GROUP BY u.unit_id
)
```

Uma passada na tabela em vez de quarenta. No plano novo:

```
-> GroupAggregate
     -> Index Only Scan using idx_user_account_unit_status on user_account
```

`Index Only Scan` — nem toca a heap.

### (b) Cast escondido no filtro

```sql
-- antes: data_id e `occurred_at::DATE` dentro de bi.fato_alerta
LEFT JOIN bi.fato_alerta f ON f.unidade_id = d.unidade_id AND f.data_id >= CURRENT_DATE - 90
```

Filtrar por uma **expressao** da coluna torna o indice em `occurred_at` inutilizavel: o Postgres
teria de aplicar o cast em cada linha para saber se ela entra — que e a definicao de `Seq Scan`.

```sql
-- depois: filtra a coluna crua, que o fato tambem expoe
LEFT JOIN bi.fato_alerta f ON f.unidade_id = d.unidade_id AND f.occurred_at >= CURRENT_DATE - 90
```

```
-> Bitmap Heap Scan on alert a  (rows=21630)
     -> Bitmap Index Scan on idx_alert_unit_occurred  (rows=21630)
```

**Este e o custo escondido de uma dimensao de tempo:** a coluna derivada (`data_id`) e otima para
`GROUP BY` e pessima para `WHERE`. Um modelo dimensional ingenuo filtra pela chave da dimensao e
perde todos os indices do fato sem que nada no SQL pareca errado.

---

## Q5 — Auditoria: heranca de tabelas e exclusao por constraint

```sql
SELECT * FROM audit_log WHERE tabela = 'alert' AND ocorrido_em >= CURRENT_DATE - 7
ORDER BY ocorrido_em DESC LIMIT 50;
```

**Antes** (48,9 ms):

```
-> Parallel Append
     -> Parallel Seq Scan on audit_log_alert         (rows=39580, loops=3)
     -> Parallel Seq Scan on audit_log_user_account  (Rows Removed by Filter: 21360)
     -> Parallel Seq Scan on audit_log_organization  (Rows Removed by Filter: 4)
     -> Parallel Seq Scan on audit_log
```

A heranca ajuda a **escrita** (cada tabela auditada grava na propria filha), mas a leitura pelo pai
nao ganha nada sozinha: o planner nao tem como saber que `tabela` e constante dentro de cada filha,
entao varre todas — inclusive a de `user_account` inteira.

**Depois** (0,103 ms):

```
Limit
  -> Merge Append
       -> Sort -> Seq Scan on audit_log            (rows=0, tabela vazia: e so o pai)
       -> Index Scan using idx_audit_alert_data on audit_log_alert  (rows=50)
```

```sql
ALTER TABLE audit_log_alert ADD CONSTRAINT audit_log_alert_tabela_check CHECK (tabela = 'alert');
-- idem para as outras filhas
CREATE INDEX idx_audit_alert_data ON audit_log_alert (ocorrido_em DESC);
```

O `CHECK` e o que informa a invariante ao planner. Com `constraint_exclusion = partition` (padrao do
Postgres para heranca), ele **poda** as filhas cujo `CHECK` contradiz o `WHERE`, e a consulta encosta
so na filha certa. O indice em `ocorrido_em DESC` elimina o `Sort`.

Dois beneficios por uma declaracao: performance **e** integridade — o `CHECK` tambem impede que a
trigger, um dia, escreva a linha de uma tabela na filha de outra (coberto por teste).

---

## Como reproduzir

```bash
# 1. Postgres limpo
docker run -d --name zera-bench -e POSTGRES_PASSWORD=zera -e POSTGRES_DB=zera \
  -p 55432:5432 postgres:16-alpine

# 2. Migracoes, em ordem
for f in $(ls src/main/resources/db/migration/V*.sql | sort -V); do
  docker exec -i zera-bench psql -U postgres -d zera -v ON_ERROR_STOP=1 -q < "$f"
done

# 3. Carga (leva ~1 min) — ja termina com ANALYZE
docker exec -i zera-bench psql -U postgres -d zera -f - < scripts/seed_bench.sql

# 4. Medir
docker exec -i zera-bench psql -U postgres -d zera \
  -c "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM bi.vw_ranking_unidades ORDER BY ranking_geral;"
```

`ANALYZE` depois de qualquer carga grande nao e opcional: sem estatisticas atualizadas o planner
decide no escuro e o plano medido nao e o plano de producao.

## Cuidado operacional ao aplicar a V15 em producao

`CREATE INDEX` (sem `CONCURRENTLY`) toma um lock que **bloqueia escrita** na tabela enquanto o indice
e construido. Em `alert` com poucos milhares de linhas isso e instantaneo; com dezenas de milhoes,
sao minutos de escrita travada — e alertas do `ms-inventory` chegam em rajada.

`CREATE INDEX CONCURRENTLY` resolve, mas **nao roda dentro de transacao**, e o Flyway envolve cada
migracao numa. As saidas, quando o volume justificar:

- marcar o script com `-- flyway:executeInTransaction=false` e usar `CONCURRENTLY`; ou
- aplicar o indice fora do Flyway, numa janela de manutencao, e deixar a migracao com
  `CREATE INDEX IF NOT EXISTS` para os ambientes pequenos.

Registrado aqui porque e o tipo de detalhe que so aparece no deploy de producao, quando ja e tarde
para escolher.

## Indices considerados e NAO criados

| Candidato | Por que nao |
| --- | --- |
| `alert (severity)` | 3 valores distintos em 118 mil linhas. Seletividade baixa demais: o planner prefere `Seq Scan` de qualquer forma. |
| `alert (kind)` | Nenhuma consulta filtra so por `kind` hoje. Indice sem consulta e custo de escrita puro. |
| `CHECK (fn_validar_cnpj(cnpj))` em `organization` | Exigiria que todo CNPJ ja gravado fosse valido, e o `legacysync` traz dado do ano anterior sem essa garantia. Adicionar derrubaria a migracao. Se for desejado: auditar primeiro, depois `NOT VALID`. |
| `idx_alert_status`, `idx_alert_created_at` (existentes, da V3) | Candidatos a remocao pelo mesmo argumento de seletividade, mas foram **mantidos**: nao ha evidencia de que ninguem os use, e remover indice de producao sem medir o antes e trocar um problema por outro. |
