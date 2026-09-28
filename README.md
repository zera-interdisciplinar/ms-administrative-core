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

Público, sem autenticação: `/actuator/health`, `/index.html`, `/api-docs`,
`/.well-known/jwks.json`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/invitations/redeem`.

## Observabilidade

- **Health**: `/actuator/health`, com probes de liveness/readiness configuradas.

## Testes

```bash
./mvnw test      # testes unitários
./mvnw verify    # + relatório e verificação de cobertura (JaCoCo, mínimo 80%)
```

Não depende de infraestrutura externa por padrão: os testes usam H2 em memória. O único teste que
sobe um PostgreSQL real (`FlywayPostgresIntegrationTest`, via Testcontainers) valida que todas as
migrações aplicam limpas e cobre objetos específicos do Postgres que o H2 não suporta (índices
parciais); é pulado automaticamente sem Docker disponível.

## Deploy

CI (`ci.yml`) roda em toda PR para `main`/`qa`: build, testes e cobertura. Deploy para QA e
produção builda a imagem e aplica os manifests em `k8s/`. Segredos e passos manuais de
provisionamento (par de chaves JWT, credenciais de serviço, seed do administrador inicial) estão
documentados em [`k8s/README.md`](k8s/README.md) — sempre a fonte da verdade para o que precisa
existir em cada ambiente antes do deploy.

## Licença

[MIT](LICENSE)
