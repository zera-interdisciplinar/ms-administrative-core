# ms-administrative-core — contexto para agentes de IA

Núcleo administrativo (Postgres): organizações, unidades, usuários, autenticação e alertas. É quem
**emite os JWT** que o `ms-inventory` e o AI core validam. Visão geral, endpoints e setup estão no
[README](README.md) — **não duplicar aqui**. Este arquivo cobre só o que não se descobre lendo o
código rápido, ou o que custa caro redescobrir.

## Comandos

```bash
./mvnw verify   # build + testes + cobertura (JaCoCo, mínimo 80%) — rode antes de abrir PR
./mvnw test     # só os testes
```

**Exige JDK 25.** O CI (`ci.yml`) só roda em PR com base `main`/`qa`, então **sempre valide
localmente**.

## Arquitetura

Hexagonal. O domínio não conhece framework; toda I/O atravessa uma porta (interface em
`core/repository`, implementação em `infrastructure/`).

```
core/domain/{entity,valueobject,exception,service}   regra de negócio pura
core/repository/                                     portas
core/usecase/<agregado>/<acao>/                      um pacote por caso de uso
infrastructure/http/{controller,request,response,handler}
infrastructure/http/client/googleplaces/             adaptador do Places (proxy, não persiste)
infrastructure/persistence/postgres/{entity,mapper,repository}
infrastructure/security/                             emissão/validação de JWT, service tokens
infrastructure/bootstrap/                            seed opcional do primeiro MANAGER
infrastructure/legacysync/                           sync opcional com o banco do ano anterior
infrastructure/scheduler/                            agendador das procedures de manutencao (off)
```

**Camada analitica e de governanca (V10-V19).** Vive no banco, nao no Java: auditoria por trigger
com heranca de tabelas, DAU automatico por trigger em `refresh_token`, functions/procedures de
regra de negocio, star schema em views no schema `bi`, catalogo de dados. O Java so **aciona**
(portas `MaintenanceRepository`, `AnalyticsRepository`, `DataCatalogRepository`, adaptadores com
`JdbcTemplate`). Detalhes em `docs/modelagem-dimensional.md`, `docs/otimizacao.md`,
`docs/catalogo-dados.md` e `docs/backup-recuperacao.md`.

Migrações: `src/main/resources/db/migration/V*.sql` (Flyway), aplicadas no boot. O schema real é o
que está nas migrações — anotação JPA **não** cria constraint, porque `ddl-auto=none`.

## Convenções

- **Comentários em português, explicando o PORQUÊ, não o quê.** Comentário que descreve o que a
  linha já diz é ruído; o que se espera é a decisão, a armadilha ou o incidente por trás do código.
  Sem acento nos comentários e mensagens de commit.
- Commits: `tipo(escopo): resumo` em pt-BR sem acento, corpo explicando o porquê.
- **Não assinar commits com `Co-Authored-By`.**
- PRs seguem o template da org (Descrição, Tipo de mudança, Task Jira, Como foi testado, Checklist).
- Testes: JUnit 5 + Mockito; `MockRestServiceServer` para clientes HTTP.

## Armadilhas (cada uma custou tempo real)

**H2 está pinado em 2.2.224 de propósito**
- O 2.4.240 tem um bug com o `CHECK` que o Hibernate gera para `@Enumerated(STRING)`: derruba
  `insert` válido com `"Check constraint invalid"`. **Não remova o `<version>` do H2** no `pom.xml`
  sem testar um `@DataJpaTest` que realmente persista um `UserJpa`.

**Spring Boot 4.1 / Java 25 — pacotes mudaram de lugar**
- `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure` (e exige a dependência
  `spring-boot-starter-data-jpa-test`, que já está no pom).
- Jackson é o **3** (`tools.jackson.databind`) nos conversores HTTP.

**Testes**
- Os testes usam **H2 em memória com `ddl-auto=create-drop`**, não as migrações. Logo, um teste
  passar **não prova** que a migração Flyway está correta. Quem cobre isso é a suíte que estende
  `AbstractPostgresIntegrationTest` (Postgres real via Testcontainers, **pulada sem Docker**) — é
  ela que pega índice parcial, `gen_random_uuid()`, `CHECK`, PL/pgSQL, trigger, `JSONB`, herança de
  tabelas, window function e CTE recursiva, nada disso existindo no H2.
- Ao adicionar migração com objeto específico de Postgres, **acrescente a asserção correspondente
  ao `FlywayPostgresIntegrationTest`**, senão ninguém percebe se ela parar de aplicar.
- **Nunca mapeie view de `bi` como `@Entity`.** O Hibernate a criaria como tabela vazia no H2 e o
  teste passaria sem executar uma linha do SQL analítico real. Leitura de `bi` é `JdbcTemplate`.
- O container do Postgres é **um só para toda a suíte** (`PostgresTestContainer`, padrão singleton).
  Não use `@Testcontainers` + `@Container` numa classe-base: a extensão **para** o container ao fim
  de cada classe, e ao reiniciá-lo o Testcontainers cria um container novo, com porta nova, enquanto
  o contexto Spring (cacheado entre classes) continua na porta antiga. O sintoma é `Connection
  refused` com timeout de pool de 30s por teste, e só aparece quando existe a **segunda** classe de
  integração.

**Chamar procedure do Postgres por JDBC**
- A forma "canônica" — `CallableStatement` com `{call sp(?, ?)}` e `registerOutParameter` — **não
  funciona**. O driver traduz a sintaxe de escape para `SELECT * FROM sp(?)`, ou seja, procura uma
  FUNCTION e descarta os parâmetros de saída. O erro engana:
  `ERROR: function sp_fechar_alertas_obsoletos(integer) does not exist ... You might need to add
  explicit type casts.` Use o `CALL` nativo — `jdbc.queryForObject("CALL sp_x(?, NULL)", ...)` — que
  devolve os `INOUT` como uma linha de resultado. Ver `MaintenanceRepositoryImpl`.
- Pelo mesmo motivo, `jdbc.update("CALL sp_x(?, NULL)")` falha com *"A result was returned when none
  was expected"*: `CALL` com `INOUT` **retorna linha**.

**Operador `?` do JSONB em PreparedStatement**
- `SELECT dados_novos ? 'password'` num `PreparedStatement` tem o `?` consumido como **placeholder
  de parâmetro**, e o erro resultante não menciona JSON em nada. Use `jsonb_exists(coluna, 'chave')`.

**Chaves JWT**
- Sem `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, a app gera um par RSA **efêmero a cada boot**. Sobe
  normalmente, mas nenhum outro serviço valida os tokens emitidos. Quase todo "o ms-inventory está
  dando 401" começa aqui, ou num `ZERA_JWT_ISSUER` divergente entre os dois serviços.

**Google Places**
- É **proxy fino, sem persistência**. Os Termos de Uso do Google permitem guardar o `place_id`
  indefinidamente, mas **não** nome/endereço/coordenada além de 30 dias — por isso o cache é curto
  e em memória. Não transforme isso em tabela.
- Desligado (`zera.places.enabled=false`) ou sem chave, o endpoint responde **503, não lista vazia**:
  lista vazia diria ao usuário "não existe recicladora perto", o que é mentira.

## Conceitos que se confundem

- **`/api/v1/recyclings`** = cadastro interno de empresas recicladoras (tem CNPJ, e-mail, endereço).
  **`/api/v1/recycling-places`** = pontos descobertos no Google Places (sem CNPJ, sem cadastro). São
  dois recursos distintos de propósito; não unifique.
- **Token de usuário** (papel `MANAGER`/`EMPLOYEE`) vs **token de serviço** (`POST /auth/service-token`,
  carrega `scope`, não papel). Rotas internas como `POST /notifications/alerts` exigem o escopo
  (`SCOPE_notifications:write`) — token de usuário **não** alcança essas rotas, por desenho.
- `user_account` é a tabela (não `user`, que é palavra reservada no Postgres). Usa herança
  single-table com `role` como discriminador; `manager_id` só vale para `EMPLOYEE`.
- **Herança single-table do JPA** (`user_account`) vs **herança de tabelas do Postgres**
  (`audit_log` → `audit_log_*`) são coisas diferentes: a primeira é mapeamento, a segunda é física.
  Em `audit_log`, o pai fica **sempre vazio** e cada tabela auditada grava na própria filha.
- `audit_log.usuario_banco` (`CURRENT_USER`) responde "qual conexão", não "qual pessoa" — a
  aplicação usa um único usuário no pool. Quem age vem do `sub` do JWT e chega ao banco por
  `SET LOCAL zera.app_user`, gravado em `usuario_app`. Por isso os adaptadores que escrevem em
  tabela auditada são `@Transactional`: sem a mesma transação, o `SET LOCAL` não alcança a escrita.

## Contrato com o ms-inventory

Mudança em qualquer um destes quebra o outro serviço silenciosamente — trate como contrato público:

- Formato/claims do JWT (inclusive o claim `name`) e o `ZERA_JWT_ISSUER`.
- `POST /api/v1/auth/service-token` e os escopos dos clientes de serviço.
- `POST /api/v1/notifications/alerts` e os valores de `AlertKind`, que espelham as regras do
  inventory.
- `GET /api/v1/users?role=MANAGER&unitId=...`, usado para achar o destinatário do alerta.

## Ao mudar regra de negócio

Pergunte antes de supor. Várias decisões de produto não estão no código — se a mudança afeta
autenticação, o contrato com o inventory ou o fluxo de alertas, confirme antes de implementar.
