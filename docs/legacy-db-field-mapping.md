# DE/PARA dos atributos do banco legado PostgreSQL do primeiro ano para o banco normalizado do segundo.

## Banco Legado
### Tabela chamada Gestor:
- codigo: serial
- nome: varchar
- email: varchar
- senha: varchar
- telefone: varchar
- cod_unidade: int
- atualizado_em: timestamp
- criado_em: timestamp

### De/para no banco normalizado:

GESTOR -> USER

- codigo -> id
- nome -> name
- email -> email
- senha -> password
- cod_unidade -> unit_id
- telefone -> telephone.number (banco está normalizado e tem tabela própria de telefone)
- atualizado_em -> updated_at
- criado_em -> created_at

(role = MANAGER sempre, status sempre ACTIVE, photo_url fica vazia até usuário preencher no app mobile)

---

### Tabela chamada Unidade:
- codigo: serial
- cnpj: varchar
- email: varchar
- cod_organizacao: int
- atualizado_em: timestamp
- criado_em: timestamp

### De/para no banco normalizado:

UNIDADE -> UNIT

- codigo -> id
- cod_organizacao -> organization_id
- atualizado_em -> updated_at
- criado_em -> created_at

(não puxamos dados de CNPJ e email)

---

### Tabela chamada Organizacao
- codigo: serial
- cnpj: varchar
- nome: varchar
- criado_em: timestamp
- atualizado_em: timestamp
- email: varchar

### De/para no banco normalizado
- codigo -> id
- cnpj -> cnpj
- nome -> name
- email -> email
- criado_em -> created_at
- atualizado_em -> updated_at
- plano.nome -> plan

A tabela plano no banco de dados legado passa por alguns relacionamentos:

Tabela Assinatura com cod_organizacao e cod_plano.

---

## Status e plano da organização (regras do sync)

O legado não tem status nem plano na tabela `organizacao`. O sync usa **a assinatura mais recente** da organização para definir os dois (`plan` e `status`). Implementação: [LegacySyncService.java](../src/main/java/com/zera/ms_administrative_core/infrastructure/legacysync/LegacySyncService.java) (`statusOf`, `planOf`) e a consulta em [JdbcLegacyReader.java](../src/main/java/com/zera/ms_administrative_core/infrastructure/legacysync/JdbcLegacyReader.java) (`LEFT JOIN LATERAL` em `assinatura` e `LEFT JOIN` em `plano`).

**Qual é a assinatura mais recente:** a de maior `data_inicio` (NULL conta como a mais antiga) e, em caso de empate, a de maior `codigo`. A coluna `data_fim` **não é lida**.

### Resultado por situação no legado

| Situação no legado | `plan` | `status` |
| --- | --- | --- |
| Assinatura mais recente com `status` = `Ativa` (maiúsculas/minúsculas não importam) | conforme `plano.nome` | `ACTIVE` |
| Assinatura mais recente com qualquer outro `status`: `Cancelada`, `Ativo`, `Ativa ` (com espaço no fim), vazio ou `NULL` | conforme `plano.nome` | `INACTIVE` |
| Organização **sem nenhuma assinatura** (o `LEFT JOIN` não a descarta) | `FREE` | `INACTIVE` |
| Assinatura sem `cod_plano`, ou `cod_plano` sem correspondente em `plano` | `FREE` | segue o `status` da assinatura |
| `plano.nome` com valor que não é `Grátis`, `Profissional` nem `Empresarial` | organização **não é sincronizada** (linha ignorada, aviso no log) | — |

- **Comparação do status:** só ignora maiúsculas/minúsculas. Não remove espaços nem acentos, e `Ativo` é diferente de `Ativa`.
- **Comparação do plano:** ignora maiúsculas/minúsculas, acentos e espaços nas pontas (`Grátis`, `gratis` e ` GRATIS ` viram `FREE`). `Profissional` vira `PROFISSIONAL` e `Empresarial` vira `EMPRESARIAL`.

### Consequências

- Cada rodada recalcula tudo. Uma organização `ACTIVE` vira `INACTIVE` quando a assinatura é cancelada ou deixa de existir, e volta a `ACTIVE` se a assinatura mais recente voltar a `Ativa`.
- O sync grava o status direto, sem passar pela validação de transições de `Status`. `SUSPENDED` nunca é produzido: uma organização suspensa localmente volta a `ACTIVE` ou `INACTIVE` na rodada seguinte.
- As ações locais de ativar, desativar, suspender e trocar plano em organizações vindas do legado são sobrescritas na rodada seguinte (o legado é a fonte da verdade).
- Se a organização for ignorada (plano desconhecido, CNPJ ou e-mail inválido), as unidades e os gestores dela também são ignorados enquanto ela não existir no banco novo (pai ausente).

## O que se espera do banco legado

Contrato que o legado precisa cumprir para o sync funcionar como esperado. O que não for cumprido não derruba a rodada: a linha é ignorada e o log mostra o tipo e o código do registro.

**Assinatura e plano**

1. `assinatura.status` deve ser exatamente `Ativa` para a assinatura vigente. Qualquer outro texto vira `INACTIVE`, inclusive variações como `Ativo`. O legado deve manter um conjunto fechado de valores (hoje a coluna aceita `NULL` e não tem `CHECK`).
2. Toda organização deve ter ao menos uma assinatura. Sem ela, a organização fica `INACTIVE` e `FREE`. `assinatura.cod_organizacao` aceita `NULL` e não tem chave estrangeira, então o legado deve garantir que aponta para uma organização existente.
3. Só a assinatura mais recente vale. Ao renovar ou trocar de plano, o legado deve criar uma assinatura com `data_inicio` maior (ou atualizar a existente). `data_inicio` aceita `NULL` no legado e deve ser sempre preenchida.
4. O novo sistema **não calcula validade**. Quando uma assinatura expirar, o legado precisa mudar o `status` dela, porque `data_fim` é ignorada. Uma assinatura vencida que continue `Ativa` mantém a organização `ACTIVE`.
5. `assinatura.cod_plano` deve ficar preenchido e `plano.nome` deve ser `Grátis`, `Profissional` ou `Empresarial`. Um plano novo exige alterar o enum `Plan` e o método `planOf`, senão as organizações desse plano deixam de sincronizar.

**Outras condições para uma linha ser sincronizada**

| Tabela | Condições | Se não cumprir |
| --- | --- | --- |
| `organizacao` | CNPJ válido (dígitos verificadores), e-mail válido e não nulo, `nome` não nulo e com até 100 caracteres, plano reconhecido | linha ignorada |
| `unidade` | `cod_organizacao` não nulo e organização já sincronizada | linha ignorada |
| `gestor` | `senha` em bcrypt (60 caracteres, começando com `$2`), e-mail válido e não usado por outro usuário, `cod_unidade` não nulo e unidade já sincronizada | linha ignorada |
| `gestor.telefone` | 10 ou 11 dígitos (a máscara é removida) | só o telefone é ignorado, o gestor é sincronizado |

**Datas:** `criado_em` e `atualizado_em` são `timestamptz` e são copiados para `created_at` e `updated_at`. Um valor `NULL` vira a data e hora do momento da rodada. O sync lê as tabelas inteiras a cada rodada, então **não depende** de `atualizado_em` para detectar mudanças.

**Situação medida em 2026-09-19 (dados de desenvolvimento):** 10 organizações, cada uma com exatamente 1 assinatura, todas com status `Ativa` e planos `Grátis`, `Profissional` e `Empresarial`. Nenhuma organização sem assinatura.