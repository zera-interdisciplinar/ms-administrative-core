# ms-administrative-core

Núcleo administrativo do sistema Zera: usuários, organizações, unidades, autenticação e
autorização entre serviços, notificações/alertas, cadastro de recicladoras parceiras e busca de
recicladoras próximas via Google Places.

## Stack

- **Java 25**
- Spring Boot 4.1.0
- PostgreSQL + Flyway (migrações versionadas em `src/main/resources/db/migration`)
- Maven (via `./mvnw`)

## Executar localmente

Requer um Postgres acessível e as variáveis de ambiente abaixo:

```bash
export DB_HOST=localhost
export DB_NAME=zera
export DB_USER=postgres
export DB_PASSWORD=<senha>

./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

### Chaves JWT

Sem `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, a aplicação gera um par RSA efêmero no boot — útil para
rodar localmente sem infraestrutura, mas nenhum outro serviço (ms-inventory, app) vai conseguir
validar os tokens emitidos, porque a chave muda a cada subida. Para integrar de verdade, gere um
par RSA (PKCS#8 para a privada, X.509/SPKI para a pública) e exporte as duas em PEM.

### Integrações opcionais (todas desligadas por padrão)

| Variável | Liga |
|---|---|
| `MS_INVENTORY_CLIENT_SECRET` | Autenticação serviço-a-serviço: o ms-inventory troca esse segredo por um token via `POST /api/v1/auth/service-token` para postar alertas em `/api/v1/notifications/alerts` |
| `LEGACY_SYNC_ENABLED=true` + `LEGACY_DB_URL`/`LEGACY_DB_USER`/`LEGACY_DB_PASSWORD` | Sincronização periódica com o banco legado (sistema do ano anterior) |
| `GOOGLE_PLACES_ENABLED=true` + `GOOGLE_PLACES_API_KEY` | Busca de recicladoras próximas via Google Places (`GET /api/v1/recycling-places`); desligada, o endpoint responde 503 em vez de inventar uma lista vazia |
| `BOOTSTRAP_ADMIN_EMAIL`/`BOOTSTRAP_ADMIN_PASSWORD`/`BOOTSTRAP_ADMIN_ORG_CNPJ` | Cria um MANAGER inicial no boot (idempotente por e-mail), para não depender de acesso direto ao banco no primeiro deploy de um ambiente |

## API

- **Swagger UI**: `/index.html`
- **Contrato OpenAPI**: `/api-docs`
- **JWKS**: `/.well-known/jwks.json` — chave pública para quem preferir validar o JWT localmente
  em vez de receber o PEM por variável de ambiente

Autenticação: JWT (RS256) emitido por este serviço. A maioria das rotas de escrita exige o papel
`MANAGER`; leituras exigem apenas um token válido; rotas de autoatendimento (trocar a própria
senha ou e-mail) aceitam o dono da conta ou um `MANAGER`. Rotas internas, chamadas por outro
serviço com um token de serviço, exigem o `scope` correspondente em vez de um papel de usuário.

| Recurso | Endpoints |
|---|---|
| Autenticação | `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/service-token` (serviço-a-serviço) |
| Convites | `/invitations` (criar), `/invitations/redeem` (usar um convite para se cadastrar) |
| Organizações | `/organization` (CRUD, plano, ativar/suspender) |
| Unidades | `/unit` |
| Usuários | `/users` (renomear, trocar gestor/e-mail/senha, ativar/suspender, contagem por gestor) |
| Telefones | `/telephone` (de usuário, organização, unidade ou recicladora) |
| Recicladoras (cadastro interno) | `/recyclings` — empresas parceiras cadastradas manualmente, com CNPJ e e-mail de contato |
| Recicladoras próximas | `/recycling-places` — proxy fino para o Google Places; **não** é o mesmo recurso que `/recyclings` (sem CNPJ, sem cadastro, só descoberta por coordenada) |
| Notificações/alertas | `/notifications/alerts` (uso interno, chamado por outros serviços) |

## Observabilidade

- `/actuator/health` (probes de liveness/readiness configuradas).

## Testes

```bash
./mvnw test
```

Cobertura mínima: 80% (JaCoCo), verificada em `./mvnw verify`. Não depende de infraestrutura
externa: os testes usam H2 em memória, e o único teste que sobe um Postgres real
(`FlywayPostgresIntegrationTest`, via Testcontainers) valida que todas as migrações aplicam
limpas e cobre objetos específicos do Postgres que o H2 não suporta (índices parciais). É pulado
automaticamente sem Docker.
