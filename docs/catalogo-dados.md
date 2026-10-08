# Catalogo de dados

Metadados tecnicos e de negocio sobre o proprio schema, em
[`V18`](../src/main/resources/db/migration/V18__create_data_catalog.sql).

## Por que uma tabela, e nao um documento

Documento de schema apodrece **em silencio**: ninguem percebe que ele divergiu, e a pessoa que
confiar nele vai errar.

Uma tabela versionada junto com as migracoes muda no mesmo commit que muda o schema e — o que
importa de verdade — pode ser **confrontada** com o `information_schema`. A funcao
`fn_catalogo_divergencia()` faz esse confronto, e o teste
`DataCatalogIntegrationTest.catalogShouldMatchSchema` **quebra o build** quando alguem adiciona coluna
sem documentar.

E isso que transforma "temos documentacao" em algo verificavel. Sem esse teste, o catalogo viraria
ficcao na primeira migracao seguinte.

## Estrutura

| Tabela | Grao |
| --- | --- |
| `catalogo_tabela` | Uma linha por tabela do dominio |
| `catalogo_coluna` | Uma linha por coluna |

Cobertura atual: **16 tabelas, 124 colunas**.

### Niveis de acesso

| Nivel | Quem pode ver | Exemplos |
| --- | --- | --- |
| `INTERNO` | Qualquer pessoa da equipe | `unit.name`, ids, timestamps |
| `RESTRITO` | Equipe do dominio | `alert.*`, `user_access_log.*` |
| `CONFIDENCIAL` | Contem dado pessoal | `user_account.email`, `address.*`, `organization.cnpj` |
| `SECRETO` | Credencial/segredo — nunca exportar | `user_account.password`, `refresh_token.token_hash`, `invitation.code` |

O nivel nao e decorativo: e a regra que as roles da
[`V17`](../src/main/resources/db/migration/V17__create_access_roles.sql) aplicam. **Catalogo sem role
e documentacao; role sem catalogo e permissao sem explicacao.** Os dois juntos sao governanca.

`contem_pii` responde uma pergunta operacional concreta: o que pode sair do banco num export ou num
dump para homologacao (ver [backup-recuperacao.md](backup-recuperacao.md#backup-que-exclui-dado-pessoal-homologacao)).

### Origem

| Origem | Significa |
| --- | --- |
| `APLICACAO` | Escrita pelos casos de uso |
| `DERIVADO` | Calculada a partir de outra tabela (`user_access_log`, `usuario_ativo_diario`) |
| `INFRAESTRUTURA` | Auditoria, catalogo, trilha de jobs |
| `LEGADO` | Existe e nao e usada — ver `disposal_report` |

## Consultas uteis

```sql
-- O que nao pode sair do banco
SELECT tabela, coluna, nivel_acesso FROM catalogo_coluna
 WHERE contem_pii = TRUE OR nivel_acesso = 'SECRETO' ORDER BY tabela, coluna;

-- Armadilhas documentadas de uma tabela
SELECT regra_negocio FROM catalogo_tabela WHERE tabela = 'alert';

-- Tabelas que existem e ninguem usa
SELECT tabela, descricao FROM catalogo_tabela WHERE origem = 'LEGADO';

-- Saude do catalogo (vazio = em dia)
SELECT * FROM fn_catalogo_divergencia();
```

Pela API (exige `MANAGER`):

```
GET /api/v1/governance/data-catalog
GET /api/v1/governance/data-catalog/{tabela}/columns
GET /api/v1/governance/data-catalog/drift
```

## Como a deteccao de divergencia funciona

`fn_catalogo_divergencia()` compara catalogo e `information_schema` **nos dois sentidos**, com CTEs:

| Tipo | Significa |
| --- | --- |
| `COLUNA_NAO_CATALOGADA` | Existe no banco, nao esta no catalogo |
| `COLUNA_INEXISTENTE` | Esta no catalogo, nao existe mais no banco |
| `TABELA_NAO_CATALOGADA` | Tabela existe no banco, nao esta no catalogo |

Os dois sentidos importam: so o primeiro deixaria passar coluna removida, e o catalogo passaria a
descrever algo que nao existe — que e como documentacao engana de forma mais convincente.

**Exclusoes deliberadas:** `flyway_schema_history` (tabela de controle do Flyway, nao dado de
dominio) e as filhas da heranca de `audit_log`. As filhas replicam exatamente as colunas do pai;
documenta-las seria copiar a mesma descricao quatro vezes — que e justamente o tipo de duplicacao que
faz documentacao divergir. A exclusao usa `pg_inherits`, entao qualquer filha nova e coberta
automaticamente.

## Manutencao

Ao alterar o schema, **no mesmo commit**:

1. Escrever a migracao (`V<n>__...sql`).
2. Adicionar/remover as linhas correspondentes em `catalogo_tabela` / `catalogo_coluna` — numa
   migracao nova, nao editando a V18, que ja foi aplicada.
3. Rodar `./mvnw verify` **com Docker**. `DataCatalogIntegrationTest` falha se ficou divergencia.

Sem Docker o teste e **pulado em silencio** e a divergencia passa. Essa e a armadilha do projeto
inteiro, nao so deste teste.

O que preencher em `regra_negocio`: a decisao, a armadilha ou o incidente por tras da coluna — nao o
tipo, que o `information_schema` ja sabe. Exemplos do que ja esta la:

- `alert.occurred_at`: "Diferente de created_at: e por esta coluna que se mede a idade do alerta,
  porque reprocessamento cria linha nova para evento antigo."
- `invitation.status`: "Nao existe EXPIRED no enum do dominio: convite vencido continua PENDING e e
  barrado por expires_at."
- `disposal_report`: "Criada na V1 e nunca usada por este servico. Documentada aqui justamente para
  que a proxima pessoa nao gaste tempo procurando o codigo que a usa — nao existe."

A terceira e o melhor exemplo do valor do catalogo: economiza uma tarde de investigacao de alguem.
