# k8s — ms-administrative-core

Manifests aplicados pelos workflows `deploy-qa` (namespace `qa`) e `deploy-prod`
(namespace `production`). Além dos manifests versionados aqui, cada ambiente
precisa de Secrets criados manualmente (uma vez) — obrigatórios para o pod
subir saudável, e opcionais para as integrações que nascem desligadas.

## 0. `postgres-secrets` (obrigatório)

Credenciais do Postgres, consumidas tanto por `postgres-qa.yaml`/`postgres.yaml` (o próprio
banco) quanto por `deployment-qa.yaml`/`deployment.yaml` (a aplicação). Sem ele nenhum dos dois
sobe.

```sh
kubectl create secret generic postgres-secrets -n qa \
  --from-literal=POSTGRES_DB=zera \
  --from-literal=POSTGRES_USER=<usuario> \
  --from-literal=POSTGRES_PASSWORD='<senha-forte>'
```

## 1. `ms-administrative-core-jwt` (obrigatório)

Par de chaves RSA usado para assinar/validar os access tokens. A chave pública
também é consumida pelo Kong e pelo `ms-inventory` (via `/.well-known/jwks.json`).

```sh
# Gerar o par (uma vez por ambiente; guarde a privada em local seguro)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl pkey -in jwt-private.pem -pubout -out jwt-public.pem

# QA
kubectl create secret generic ms-administrative-core-jwt -n qa \
  --from-file=private.pem=jwt-private.pem \
  --from-file=public.pem=jwt-public.pem

# Produção
kubectl create secret generic ms-administrative-core-jwt -n production \
  --from-file=private.pem=jwt-private.pem \
  --from-file=public.pem=jwt-public.pem
```

Rotação: gere um novo par, atualize o Secret e faça `kubectl rollout restart`.
Tokens emitidos com a chave antiga deixam de valer (o `kid` muda).

## Credenciais de serviço

As rotas internas (hoje `POST /api/v1/notifications/alerts`) exigem um token de serviço, obtido em
`POST /api/v1/auth/service-token` com as credenciais do cliente. O segredo de cada cliente fica num
Secret e entra no pod como variável de ambiente.

```bash
# gere um segredo forte por cliente e por ambiente
openssl rand -base64 48 > ms-inventory-secret.txt

# QA
kubectl create secret generic zera-service-clients -n qa \
  --from-file=ms-inventory=ms-inventory-secret.txt

# Produção
kubectl create secret generic zera-service-clients -n production \
  --from-file=ms-inventory=ms-inventory-secret.txt
```

O mesmo valor precisa ser configurado no ms-inventory, que o usa para pedir o token.

Rotação: gere um novo segredo, atualize o Secret **nos dois serviços** e faça `kubectl rollout
restart` em ambos. Tokens já emitidos continuam valendo até expirar (15 minutos por padrão).

## 2. `ms-administrative-core-bootstrap` (opcional)

Consumido **apenas** pelo `InitialManagerSeeder` no startup, que cria a árvore
`organization → unit → MANAGER` inicial. Sem este Secret o seeder é no-op.
Roda em todo boot, idempotente pelo email: rodar de novo com um email já
existente não faz nada.

Chaves (todas como variáveis de ambiente `BOOTSTRAP_ADMIN_*`):

| chave                        | obrigatória | default             |
|------------------------------|-------------|---------------------|
| `BOOTSTRAP_ADMIN_EMAIL`      | sim         | —                   |
| `BOOTSTRAP_ADMIN_PASSWORD`   | sim         | —                   |
| `BOOTSTRAP_ADMIN_ORG_CNPJ`   | sim         | — (CNPJ válido)     |
| `BOOTSTRAP_ADMIN_NAME`       | não         | `Administrador`     |
| `BOOTSTRAP_ADMIN_ORG_NAME`   | não         | `Organizacao Padrao`|
| `BOOTSTRAP_ADMIN_ORG_EMAIL`  | não         | = `BOOTSTRAP_ADMIN_EMAIL` |
| `BOOTSTRAP_ADMIN_ORG_PLAN`   | não         | `FREE`              |
| `BOOTSTRAP_ADMIN_UNIT_NAME`  | não         | `Matriz`            |

```sh
kubectl create secret generic ms-administrative-core-bootstrap -n production \
  --from-literal=BOOTSTRAP_ADMIN_EMAIL=admin@zera.com \
  --from-literal=BOOTSTRAP_ADMIN_PASSWORD='<senha-forte>' \
  --from-literal=BOOTSTRAP_ADMIN_ORG_CNPJ=11222333000181
```

Depois que o primeiro MANAGER existir e conseguir logar, o Secret pode ser
removido — ele só é lido no `flyway migrate` do boot.

## 3. `google-places` (opcional — recicladoras próximas)

`GET /api/v1/recycling-places` usa a Google Places API (New). O deployment já manda
`GOOGLE_PLACES_ENABLED=true`, e a chave é `optional: true`: sem o Secret, o pod sobe normalmente
e o endpoint responde 503 em vez de crash-loop — mesmo padrão do resto das integrações opcionais.

```sh
kubectl create secret generic google-places -n qa \
  --from-literal=api-key='<chave-da-api-do-google-places>'

kubectl create secret generic google-places -n production \
  --from-literal=api-key='<chave-da-api-do-google-places>'
```

A chave é gerada no Console do GCP (Places API, New), restrita por IP do cluster (a chamada sai
do servidor, não do app do usuário). Custo por chamada — ver `zera.places.cache-ttl` em
`application.properties` para o TTL do cache que reduz chamadas repetidas (6h por padrão; nome,
endereço e coordenada não podem ser cacheados além de 30 dias pelos Termos de Uso do Google).
