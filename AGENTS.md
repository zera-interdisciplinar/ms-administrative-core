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
```

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
  passar **não prova** que a migração Flyway está correta. Quem cobre isso é o
  `FlywayPostgresIntegrationTest` (Postgres real via Testcontainers, pulado sem Docker) — é o único
  que pega índice parcial, `gen_random_uuid()` e `CHECK`, que o H2 não suporta.
- Ao adicionar migração com objeto específico de Postgres, **acrescente a asserção correspondente
  nesse teste**, senão ninguém percebe se ela parar de aplicar.

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
