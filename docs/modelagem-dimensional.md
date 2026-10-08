# Modelagem dimensional (camada `bi`)

Schema `bi`, criado na [`V15`](../src/main/resources/db/migration/V15__create_bi_star_schema.sql).
Tudo aqui e **view**, nao tabela.

## Decisoes e o porque

### View, nao tabela materializada

O volume do nucleo administrativo e pequeno (organizacoes, unidades, usuarios) e o dado precisa
estar atualizado no segundo. Materializar exigiria um processo de refresh, uma janela de defasagem e
um lugar novo para o refresh falhar em silencio.

Caminho de crescimento, se o volume mudar: trocar `CREATE VIEW` por `CREATE MATERIALIZED VIEW` e
chamar `REFRESH` dentro da `sp_consolidar_dau`. **A interface de consulta nao muda** — nenhum
dashboard precisa ser reescrito.

### Snowflake, nao Star puro

`dim_unidade` referencia `dim_organizacao` em vez de repetir plano e status da organizacao em
cada linha de unidade. A hierarquia organizacao → unidade ja e real no OLTP; desnormalizar criaria
duas fontes para o mesmo atributo, e duas fontes divergem.

### Schema separado

Porque o **nivel de acesso** e diferente. A role `zera_bi_leitor` ([`V17`](../src/main/resources/db/migration/V17__create_access_roles.sql))
ve `bi` e nao ve `public`.

Isso funciona por um detalhe do Postgres que vale entender: **uma view executa com os privilegios de
quem a criou**, nao de quem a consulta. Entao a ferramenta de BI le `bi.fato_alerta` sem ter — e sem
poder ganhar — qualquer acesso a tabela `alert` ou a `user_account`. E a diferenca pratica entre "o
BI ve os numeros" e "o BI ve o hash de senha de todo mundo". Verificado em teste
(`FlywayPostgresIntegrationTest.biRoleReadsAnalyticsButNotOperationalTables`).

## Diagrama

```
                    ┌───────────────────────┐
                    │   bi.dim_organizacao  │
                    │ organizacao_id (PK)   │
                    │ organizacao, plano    │
                    │ status, data_cadastro │
                    └───────────▲───────────┘
                                │ organizacao_id        (Snowflake: a dimensao
                    ┌───────────┴───────────┐            aponta para outra dimensao)
                    │    bi.dim_unidade     │
                    │ unidade_id (PK)       │
                    │ unidade, cidade, uf   │
                    └───────────▲───────────┘
                                │
      ┌─────────────────┐       │       ┌──────────────────┐
      │  bi.dim_tempo   │       │       │  bi.dim_usuario  │
      │ data_id (PK)    │       │       │ usuario_id (PK)  │
      │ ano, mes, ...   │       │       │ papel, gestor    │
      └────────▲────────┘       │       └────────▲─────────┘
               │                │                │
               │     ┌──────────┴────────────────┴──────────┐
               ├─────┤          bi.fato_alerta              │
               │     │ grao: 1 alerta                       │
               │     │ medidas: quantidade, peso_severidade,│
               │     │          aberto, horas_ate_fechamento│
               │     └──────────────────────────────────────┘
               │
               │     ┌──────────────────────────────────────┐
               └─────┤          bi.fato_acesso              │
                     │ grao: 1 evento de autenticacao       │
                     │ medida: quantidade                   │
                     └──────────────────────────────────────┘
```

## Dimensoes

| View | Grao | Observacao |
| --- | --- | --- |
| `bi.dim_tempo` | 1 dia | Gerada por **CTE recursiva**; o inicio acompanha o dado (ver abaixo). |
| `bi.dim_organizacao` | 1 organizacao | Espelha `organization`, **sem CNPJ e e-mail**: sao dado pessoal/sensivel e nenhum grafico os agrupa. Quem precisar do CNPJ consulta o OLTP com a role adequada. |
| `bi.dim_unidade` | 1 unidade | `DISTINCT ON` no endereco (ver abaixo). |
| `bi.dim_usuario` | 1 usuario | Traz o nome do gestor por auto-join. |

### Por que `dim_tempo` contem dias sem fato

Uma dimensao de tempo precisa conter **todo** dia do periodo, inclusive os sem movimento. E isso que
permite um grafico mostrar "zero acessos na terca" em vez de pular a terca. Sem a dimensao completa,
uma queda a zero fica **invisivel** — o pior tipo de erro num painel, porque parece que nao houve
problema.

`generate_series` faria o mesmo; a recursao e usada por ser o que o requisito pede e por deixar o
passo (`dia + 1`) explicito.

**O inicio do calendario e dirigido pelos dados, nao uma constante — e isso e um bug corrigido, nao
preferencia de estilo.** As views analiticas fazem `JOIN` com esta dimensao, e `JOIN` descarta o que
nao casa. Com o inicio fixo em 2024-01-01, todo alerta anterior a essa data **sumia** da
`bi.vw_alertas_mensal_unidade`: sem erro, sem aviso, so um total menor. Verificado num banco limpo —
o fato mostrava 2 alertas e a view mensal, 1.

E o pior tipo de defeito num painel, porque o numero errado **parece plausivel**: ninguem desconfia
de um total que simplesmente veio menor. So um teste que compara o FATO com a VIEW pega isso, e e
exatamente o que `BiViewsIntegrationTest.shouldNotDropFactsOlderThanCalendarStart` faz.

Ha um piso de `2000-01-01` do outro lado: uma data absurda em `occurred_at` (typo de ano, dado
legado sujo) geraria milhoes de linhas de calendario a cada consulta.

### Por que `dim_unidade` usa `DISTINCT ON`

`address.unit_id` nao tem restricao de unicidade no schema: uma unidade **pode** ter dois enderecos.
Sem o `DISTINCT ON`, a unidade apareceria duas vezes na dimensao e **todo fato ligado a ela seria
contado em dobro** — o classico fan-out de join, que produz numeros plausiveis e errados.

Coberto por teste (`BiViewsIntegrationTest.shouldNotDuplicateUnitWithTwoAddresses`), que tambem
verifica que fica o endereco mais recente.

## Fatos

### `bi.fato_alerta` — grao: um alerta

| Medida | Definicao | Por que existe |
| --- | --- | --- |
| `quantidade` | sempre 1 | Permite `SUM` em vez de `COUNT`, uniformizando a agregacao. |
| `peso_severidade` | HIGH=9, MEDIUM=3, LOW=1 | 10 alertas LOW nao equivalem a 10 HIGH; um `COUNT` trataria igual. |
| `aberto` | 1 se `status = 'OPEN'` | Medida aditiva: `SUM(aberto)` da o backlog. |
| `horas_ate_fechamento` | `updated_at - occurred_at`, so se fechado | Nulo para alerta aberto — nulo e a resposta correta, nao zero. |

> A escala 1/3/9 e a mesma de `fn_indice_saude_unidade` e espelha a ordenacao de severidade do
> `ms-inventory`. **Mudar aqui sem mudar la faz o painel discordar do alerta.**

### `bi.fato_acesso` — grao: um evento de autenticacao

Traz `unidade_id` do usuario para permitir DAU por unidade sem a ferramenta de BI navegar duas
dimensoes.

## Views analiticas (ETL em SQL)

Cada uma organiza a transformacao em **CTEs** e calcula com **window functions**.

### `bi.vw_alertas_mensal_unidade`

| Coluna | Funcao de janela | Pergunta que responde |
| --- | --- | --- |
| `total_acumulado` | `SUM() OVER (PARTITION BY unidade ORDER BY mes ROWS UNBOUNDED PRECEDING)` | "Quanto essa unidade acumulou ate este mes?" |
| `ranking_no_mes` | `DENSE_RANK() OVER (PARTITION BY mes ORDER BY total DESC)` | "Qual a posicao dela entre as unidades **naquele** mes?" |
| `media_movel_3m` | `AVG() OVER (... ROWS 2 PRECEDING)` | "Qual a tendencia, sem o ruido de um mes atipico?" |
| `total_mes_anterior` | `LAG() OVER (PARTITION BY unidade ORDER BY mes)` | "Piorou ou melhorou?" |

Nenhuma delas e expressavel com `GROUP BY`: todas precisam ver as **outras** linhas do resultado.

Estrutura: CTE `base` agrega no grao (mes, unidade) e e a unica que toca o fato; CTE `janelas` faz os
calculos analiticos sobre o resultado agregado. Separar assim e o que mantem a view legivel — e
tambem o que deixa claro onde o custo esta.

### `bi.vw_dau_diario`

`LEFT JOIN` **a partir de** `dim_tempo`, para dia sem acesso aparecer com zero (ver acima).

`usuarios_unicos_7d` usa `LEFT JOIN LATERAL` e nao window function porque **`COUNT(DISTINCT)` nao e
suportado como funcao de janela no Postgres** — limitacao real do banco, nao escolha de estilo. Vale
saber disso antes de tentar `COUNT(DISTINCT x) OVER (...)` e levar um erro de sintaxe confuso.

Colunas: `dau`, `acessos`, `dau_media_movel_7d`, `acessos_acumulados`, `variacao_dod`,
`ranking_no_mes`, `usuarios_unicos_7d`, `aderencia_dau_wau` (DAU/WAU, indicador de recorrencia).

### `bi.vw_ranking_unidades`

Janela de 90 dias. `RANK`, `DENSE_RANK` por organizacao, `NTILE(4)`, `PERCENT_RANK` e participacao
percentual sobre o total.

`NTILE(4)` divide as unidades em quartis de gravidade: e o corte que responde "quais 25% estao pior"
**sem escolher um limiar arbitrario de alertas na mao** — o limiar sai dos dados.

`peso_por_usuario` normaliza por usuario ativo: unidade grande gera mais alerta por tamanho, nao por
estar pior. Sem normalizar, o ranking so ordenaria unidades por numero de funcionarios.

> Esta view foi reescrita na V16 por performance (980 ms → 33 ms). Ver [otimizacao.md](otimizacao.md#q4).

### `bi.vw_hierarquia_equipe`

**CTE recursiva** sobre `user_account.manager_id`: nivel, caminho legivel (`Gestor > Coordenador >
Funcionario`), id da raiz e tamanho da equipe.

Responde "quem esta sob quem, e em que nivel" — pergunta que exige numero **variavel** de joins e por
isso e impossivel em SQL nao recursivo.

O array `caminho` corta a recursao se um id reaparecer. Isso nao e zelo excessivo: `manager_id` e
auto-referencia **sem protecao anti-ciclo no schema**, entao A → B → A e fisicamente possivel, e sem
o corte quem cai e o servidor, nao a consulta. Coberto por teste
(`BusinessFunctionsIntegrationTest.shouldSurviveCycle`).

## Como ligar uma ferramenta de BI

```sql
-- 1. conta real, com senha (nunca numa migracao versionada)
CREATE ROLE bi_metabase LOGIN PASSWORD '<segredo do cofre>';

-- 2. concede o papel
GRANT zera_bi_leitor TO bi_metabase;
```

A role `zera_bi_leitor` e `NOLOGIN` de proposito: e um **papel**, nao uma conta. Toda view criada em
`bi` daqui em diante ja nasce legivel por ela (`ALTER DEFAULT PRIVILEGES`), o que evita o `GRANT`
manual que ninguem lembra de fazer — e que quebraria o painel em producao.

## Endpoints equivalentes

Quem nao vai conectar direto no banco consome pela API (todos exigem `MANAGER`):

| Rota | View / function |
| --- | --- |
| `GET /api/v1/analytics/units/{id}/alerts/monthly` | `bi.vw_alertas_mensal_unidade` |
| `GET /api/v1/analytics/units/ranking` | `bi.vw_ranking_unidades` |
| `GET /api/v1/analytics/dau` | `bi.vw_dau_diario` |
| `GET /api/v1/analytics/units/{id}/health` | `fn_indice_saude_unidade` |
| `GET /api/v1/analytics/managers/{id}/team-size` | `fn_tamanho_equipe` (CTE recursiva, equipe inteira) |

> **Pendencia de produto:** a v1 nao restringe o gestor as unidades da propria organizacao — o
> ranking sai global. Inofensivo enquanto o produto e mono-organizacao; no momento em que houver duas
> organizacoes reais, isto precisa filtrar pela organizacao do token.
