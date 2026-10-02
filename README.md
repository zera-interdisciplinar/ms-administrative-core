# ms-administrative-core

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Núcleo administrativo do sistema Zera: organizações, unidades, usuários, autenticação e
autorização (inclusive entre serviços), notificações/alertas, cadastro de recicladoras parceiras
e busca de recicladoras próximas via Google Places. É o serviço que emite os tokens que o
[`ms-inventory`](https://github.com/zera-interdisciplinar/ms-inventory) e o
`ms-artificial-intelligence-core` validam.

## Sumário

- [Domínio](#domínio)
- [Arquitetura](#arquitetura)
- [Stack](#stack)
- [Executando localmente](#executando-localmente)
- [Configuração](#configuração)
- [API](#api)
- [Analítico, auditoria e governança](#analítico-auditoria-e-governança)
- [Observabilidade](#observabilidade)
- [Testes](#testes)
- [Deploy](#deploy)

## Domínio

Hierarquia principal: uma **organização** (empresa cliente, com CNPJ e um **plano** —
`FREE`/`PROFISSIONAL`/`EMPRESARIAL`) tem uma ou mais **unidades**; cada unidade tem **usuários**,
que são `MANAGER` (gestor, acesso administrativo) ou `EMPLOYEE` (operário, uso do dia a dia).

Um usuário entra no sistema de duas formas: cadastro direto (gestor cria a organização) ou por
**convite** — um `MANAGER` gera um código (`POST /invitations`), a pessoa se cadastra com ele
(`POST /invitations/redeem`) e já entra vinculada à unidade e ao gestor corretos, sem precisar de
outro `MANAGER` para aprovar.

Autenticação é feita com um par de tokens JWT (RS256): um **access token** de vida curta (padrão
15 minutos) e um **refresh token** de vida longa (padrão 7 dias, persistido e revogável). Serviços
como o ms-inventory não usam login de usuário para chamar rotas internas — eles trocam um
`clientId`/`clientSecret` próprio por um **token de serviço** (`POST /auth/service-token`), sem
refresh: pedem outro quando expira.

Um **alerta** (`Alert`) é a notificação que chega ao usuário — hoje, majoritariamente disparada
pelo ms-inventory via a rota de serviço-a-serviço (`POST /notifications/alerts`), cobrindo desde
avisos de garantia vencendo até aprovação/reprovação de um cadastro de item. Tem um `kind`
(tipo), uma `severity` (`LOW`/`MEDIUM`/`HIGH`) e o `unitId`/`userId` de destino.

**Recicladora** é uma empresa parceira cadastrada manualmente (nome, CNPJ, e-mail de contato),
distinta de **recicladora próxima**, que é um ponto encontrado via Google Places — sem CNPJ, sem
cadastro, só descoberta por coordenada geográfica. São dois conceitos e dois endpoints
propositalmente separados.

## Arquitetura

Hexagonal: o domínio não depende de framework, e toda I/O passa por uma porta.

```
src/main/java/com/zera/ms_administrative_core/
├── core/
│   ├── domain/
│   │   ├── entity/         User (Manager/Employee), Organization, Unit, Alert, RecyclingBusiness, ...
│   │   ├── valueobject/    Role, Plan, Severity, AlertKind, GeoCoordinate, ...
│   │   └── exception/      Exceções de domínio (mapeadas para HTTP no handler)
│   ├── repository/         Portas — UserRepository, OrganizationRepository, RecyclingPlaceGateway, ...
│   └── usecase/            Um pacote por caso de uso (organization, user, telephone, recyclingPlace, ...)
└── infrastructure/
    ├── http/
    │   ├── controller/     Um controller por recurso
    │   └── client/googleplaces/  Cliente do Google Places (adaptador da porta de busca de recicladoras)
    ├── persistence/postgres/   Entidades JPA, mappers e adaptadores das portas de repositório
    ├── security/           Emissão/validação de JWT, service tokens, regras de autorização
    ├── bootstrap/          Seed do primeiro MANAGER de um ambiente novo (opcional, idempotente)
    └── legacysync/         Sincronização periódica com o banco do sistema do ano anterior (opcional)
```

Migrações do schema vivem em `src/main/resources/db/migration/` (Flyway), aplicadas
automaticamente no boot.

## Stack

| | |
|---|---|
| Linguagem | **Java 25** |
| Framework | Spring Boot 4.1.0 (Web MVC, Security, Actuator) |
| Banco | PostgreSQL + Flyway |
| Documentação da API | springdoc-openapi |
| Build | Maven (via `./mvnw`, sem instalação local necessária) |

## Executando localmente

Pré-requisitos: JDK 25 e um PostgreSQL acessível (local, Docker ou remoto).

```bash
export DB_HOST=localhost
export DB_NAME=zera
export DB_USER=postgres
export DB_PASSWORD=<senha>

./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

### Sobre a chave JWT em dev

Sem `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, a aplicação gera um par RSA efêmero a cada boot — permite
rodar isoladamente, mas **nenhum outro serviço vai conseguir validar os tokens emitidos**, porque
a chave muda a cada subida. Para testar a integração de verdade com o ms-inventory (ou qualquer
outro consumidor), gere um par fixo:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl pkey -in jwt-private.pem -pubout -out jwt-public.pem

export JWT_PRIVATE_KEY="$(cat jwt-private.pem)"
export JWT_PUBLIC_KEY="$(cat jwt-public.pem)"
```

A chave pública também é servida em `/.well-known/jwks.json`, para quem preferir buscá-la em
runtime em vez de recebê-la por variável de ambiente.

## Configuração

Toda configuração sensível vem de variável de ambiente; não há segredo com valor padrão no
código. Referência completa em `src/main/resources/application.properties`; provisionamento de
Secrets em produção/QA está em [`k8s/README.md`](k8s/README.md).

### Obrigatórias

| Variável | Efeito |
|---|---|
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Par RSA para assinar/validar os access tokens — ver seção acima |

### Integrações e recursos opcionais (desligados por padrão)

| Variável | Liga |
|---|---|
| `MS_INVENTORY_CLIENT_SECRET` | Permite ao ms-inventory obter um token de serviço (`POST /auth/service-token`) e postar alertas em `/notifications/alerts` |
| `LEGACY_SYNC_ENABLED=true` + `LEGACY_DB_URL`/`LEGACY_DB_USER`/`LEGACY_DB_PASSWORD` | Sincronização periódica com o banco do sistema legado |
| `GOOGLE_PLACES_ENABLED=true` + `GOOGLE_PLACES_API_KEY` | Busca de recicladoras próximas via Google Places (`GET /recycling-places`); desligada, o endpoint responde 503 em vez de uma lista vazia enganosa |
| `MAINTENANCE_ENABLED=true` | Agenda as procedures de manutenção (revogação de tokens vencidos e consolidação de DAU). Desligado por padrão porque, com mais de uma réplica, todas rodariam a mesma procedure no mesmo minuto — as procedures são idempotentes, então o resultado segue correto, mas o trabalho é duplicado. Alternativa: agendador externo chamando `POST /maintenance/*` |
| `CLOSE_STALE_ALERTS_ENABLED=true` | Inclui o fechamento automático de alertas antigos no agendador. Separado do anterior de propósito: `alert.status` é observado pelo ms-inventory, e fechar alerta sozinho é decisão de produto |
| `BOOTSTRAP_ADMIN_EMAIL`/`PASSWORD`/`ORG_CNPJ` (+ opcionais `_NAME`, `_ORG_NAME`, `_ORG_EMAIL`, `_ORG_PLAN`, `_UNIT_NAME`) | Cria a árvore organização → unidade → MANAGER inicial no boot, idempotente por e-mail — útil para o primeiro deploy de um ambiente sem acesso direto ao banco |

## API

- **Swagger UI**: `/index.html`
- **Contrato OpenAPI**: `/api-docs`
- **JWKS**: `/.well-known/jwks.json`

Autenticação: JWT (RS256) emitido por este serviço. Rotas de escrita/administrativas exigem o
papel `MANAGER`; leituras exigem apenas um token válido; rotas de autoatendimento (trocar a
própria senha ou e-mail) aceitam o dono da conta **ou** um `MANAGER`; rotas internas exigem um
token de serviço com o `scope` correspondente, não um papel de usuário.

| Recurso | Endpoints | Quem chama |
|---|---|---|
| Autenticação | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/service-token` | Público (login/refresh/logout); service-token exige credencial de cliente de serviço |
| Convites | `POST /invitations`, `POST /invitations/redeem` | Gerar: gestor. Resgatar: público (é o próprio cadastro) |
| Organizações | `GET /organization`, `GET /organization/{id}`, `POST /organization`, `PATCH /organization/{id}/{rename,email,plan,activate,deactivate,suspend}` | Ler: qualquer autenticado. Escrever: gestor |
| Unidades | `POST /unit`, `GET /unit`, `GET /unit/{id}`, `PATCH /unit/{id}/rename`, `DELETE /unit/{id}` | Ler: qualquer autenticado. Escrever: gestor |
| Usuários | `GET /users`, `GET /users/{id}`, `GET /users/count-by-manager`, `PATCH /users/{id}/{rename,manager,activate,deactivate,suspend}`, `PATCH /users/{id}/{email,password}` | Leitura/listagem e a maioria das trocas: gestor. Trocar a própria senha/e-mail: dono da conta ou gestor |
| Telefones | `GET /telephone`, `GET /telephone/{organization,user,unit,recyclings}`, `POST /telephone/{user,organization,unit,recyclings}`, `PATCH /telephone/{id}/number`, `DELETE /telephone/{id}` | Ler: qualquer autenticado. Escrever: gestor |
| Recicladoras (cadastro interno) | `POST /recyclings`, `GET /recyclings`, `GET /recyclings/{id}`, `GET /recyclings/cnpj/{cnpj}`, `PATCH /recyclings/{id}/{name,email}` | Ler: qualquer autenticado. Escrever: gestor |
| Recicladoras próximas | `GET /recycling-places?lat=&lng=&radiusMeters=` | Qualquer autenticado — proxy fino para o Google Places, sem cadastro nem CNPJ |
| Notificações/alertas | `POST /notifications/alerts` | Uso interno, exige token de serviço com escopo `notifications:write` |
| Analytics (BI) | `GET /analytics/units/{id}/alerts/monthly`, `GET /analytics/units/ranking`, `GET /analytics/dau`, `GET /analytics/units/{id}/health` | Gestor. Lê as views dimensionais do schema `bi` e a function de saúde da unidade |
| Manutenção | `POST /maintenance/alerts/close-stale`, `POST /maintenance/tokens/revoke-expired`, `POST /maintenance/dau/consolidate` | Gestor **ou** token de serviço com escopo `maintenance:write`. Aciona as procedures do banco |
| Governança | `GET /governance/data-catalog`, `GET /governance/data-catalog/{tabela}/columns`, `GET /governance/data-catalog/drift` | Gestor. Catálogo de dados e divergência entre catálogo e schema real |

Público, sem autenticação: `/actuator/health`, `/index.html`, `/api-docs`,
`/.well-known/jwks.json`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/invitations/redeem`.

## Analítico, auditoria e governança

Camada implementada dentro do Postgres (migrações `V10`–`V17`), acionável pela API ou consultável
direto no banco:

| O que | Onde | Documentação |
|---|---|---|
| Trilha de auditoria de escrita (`INSERT`/`UPDATE`/`DELETE` em `user_account`, `alert`, `organization`), com payload `OLD`/`NEW` em JSONB, usuário de banco e usuário de aplicação | `audit_log` + tabelas filhas por herança | [catalogo-dados.md](docs/catalogo-dados.md) |
| Registro automático de DAU — todo login vira evento de acesso por trigger em `refresh_token`, sem a aplicação pedir | `user_access_log`, `usuario_ativo_diario` | [modelagem-dimensional.md](docs/modelagem-dimensional.md) |
| Modelagem dimensional (Snowflake) para BI: 4 dimensões, 2 fatos e 4 views analíticas com window functions | schema `bi` | [modelagem-dimensional.md](docs/modelagem-dimensional.md) |
| Functions e procedures de regra de negócio | `fn_*`, `sp_*` | [modelagem-dimensional.md](docs/modelagem-dimensional.md) |
| Catálogo de dados com regra de negócio e nível de acesso por coluna, confrontável com o schema real | `catalogo_tabela`, `catalogo_coluna`, `fn_catalogo_divergencia()` | [catalogo-dados.md](docs/catalogo-dados.md) |
| Índices derivados de `EXPLAIN ANALYZE`, com medições antes/depois | `V15` | [otimizacao.md](docs/otimizacao.md) |
| Backup, restauração, PITR e ensaio de recuperação | `scripts/backup.sh` | [backup-recuperacao.md](docs/backup-recuperacao.md) |

Para ligar uma ferramenta de BI sem dar acesso a dado pessoal, use a role `zera_bi_leitor`: ela
enxerga o schema `bi` e **não** enxerga `public` — uma view roda com os privilégios de quem a criou,
então os números saem sem que o BI alcance hash de senha. Ver
[modelagem-dimensional.md](docs/modelagem-dimensional.md#como-ligar-uma-ferramenta-de-bi).

## Observabilidade

- **Health**: `/actuator/health`, com probes de liveness/readiness configuradas.
- **Execução das rotinas de manutenção**: tabela `job_execucao` (quando rodou, quanto afetou, se
  deu certo) — responde "há quanto tempo esse job não roda?" sem depender de log de aplicação.
- **DAU**: `GET /analytics/dau` ou a view `bi.vw_dau_diario`.

## Testes

```bash
./mvnw test      # testes unitários
./mvnw verify    # + relatório e verificação de cobertura (JaCoCo, mínimo 80%)
```

Os testes unitários usam H2 em memória e **não** aplicam as migrações (`ddl-auto=create-drop`) —
um teste unitário passar não prova que a migração está correta.

Quem prova isso é a suíte de integração, que sobe um PostgreSQL real via Testcontainers
(um único container compartilhado por todas as classes):

| Classe | Cobre |
|---|---|
| `FlywayPostgresIntegrationTest` | Todas as migrações aplicam; existência de functions, procedures, triggers, herança, views `bi`, índices e roles |
| `AuditTriggerIntegrationTest` | `TG_OP`, `OLD`/`NEW`, mascaramento de senha, `CURRENT_USER`, usuário de aplicação, herança de tabelas |
| `DauTriggerIntegrationTest` | Registro automático de acesso e contagem correta de DAU (`COUNT(DISTINCT)`) |
| `BusinessFunctionsIntegrationTest` | `fn_validar_cnpj`, `fn_tamanho_equipe` (CTE recursiva, inclusive com ciclo), `fn_indice_saude_unidade` |
| `BusinessProceduresIntegrationTest` | As três procedures e o adaptador que as aciona, incluindo idempotência |
| `BiViewsIntegrationTest` | Valores esperados das window functions (running total, `LAG`, média móvel) e ausência de fan-out |
| `DataCatalogIntegrationTest` | Catálogo em dia com o schema real |

**Sem Docker esses testes são pulados em silêncio.** Rode `./mvnw verify` com Docker antes de abrir
PR, senão a suíte passa sem exercitar nada do que está no banco.

## Deploy

CI (`ci.yml`) roda em toda PR para `main`/`qa`: build, testes e cobertura. Deploy para QA e
produção builda a imagem e aplica os manifests em `k8s/`. Segredos e passos manuais de
provisionamento (par de chaves JWT, credenciais de serviço, seed do administrador inicial) estão
documentados em [`k8s/README.md`](k8s/README.md) — sempre a fonte da verdade para o que precisa
existir em cada ambiente antes do deploy.

## Licença

[MIT](LICENSE)
