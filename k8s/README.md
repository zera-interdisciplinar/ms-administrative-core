# k8s — ms-administrative-core

Manifests aplicados pelos workflows `deploy-qa` (namespace `qa`) e `deploy-prod`
(namespace `production`). Além dos manifests versionados aqui, cada ambiente
precisa de Secrets criados manualmente (uma vez) — obrigatórios para o pod
subir saudável, e opcionais para as integrações que nascem desligadas.

## Redis (fila de alertas, obrigatório)

`redis.yaml`/`redis-qa.yaml` sobem um Redis standalone chamado **`redis-alerts`** (sem
persistência — a fila de alertas é transiente por natureza, perder o que não foi confirmado num
restart é aceitável). Aplicados a cada deploy pelos workflows `deploy-qa`/`deploy-prod` (`kubectl
apply` é idempotente: reaplicar sem mudança não faz nada). Para subir manualmente antes do
primeiro deploy, ou fora do pipeline:

```sh
# QA
kubectl apply -f k8s/redis-qa.yaml -n qa

# Produção
kubectl apply -f k8s/redis.yaml -n production
```

`deployment.yaml`/`deployment-qa.yaml` já apontam `REDIS_HOST=redis-alerts` e `REDIS_PORT=6379`
para o Service acima — não é Secret, é valor fixo (nome do Service dentro do mesmo namespace).
Sem o Redis no ar, a fila fica ligada por padrão (`zera.alerts.queue.enabled=true`) e o
`POST /api/v1/notifications/alerts` passa a falhar ao tentar enfileirar.

**Por que `redis-alerts` e não `redis`:** o namespace `qa`/`production` é compartilhado entre os
microsserviços, e o `ms-artificial-intelligence-core` já tem seu próprio Deployment/Service
chamado `redis` (com `redis-stack-server` e PVC, para checkpoint do LangGraph). Na primeira versão
deste manifest usamos o mesmo nome por engano, e o `kubectl apply` do nosso deploy sobrescreveu o
deles — o deploy deste serviço trocou a imagem e removeu o volume do Redis do AI core, quebrando-o
(o PVC `redis-pvc` deles não foi afetado, só o Deployment/Service; reaplicar o `redis.yaml`/`redis-
qa.yaml` deles restaura o estado correto). **Nunca reuse um nome de Deployment/Service/PVC sem
primeiro checar os manifests dos outros serviços no mesmo namespace.**

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

## 4. `fcm-credentials` (opcional — push notification de alerta)

`NotifyUserImpl` manda push via FCM além de gravar o alerta (best-effort: falha no FCM nunca
derruba o alerta). O deployment já manda `FCM_ENABLED=true`, e a chave é `optional: true`: sem o
Secret (ou com ele vazio), o `FcmConfig` cai no `NoopPushNotificationGateway` em vez de crash loop
no boot — mesmo padrão do Google Places.

A credencial é o JSON **inteiro** da service account do Firebase (Console do Firebase →
Configurações do projeto → Contas de serviço → Gerar nova chave privada), não uma chave simples
como as demais integrações — por isso `--from-file`, não `--from-literal`.

```sh
kubectl create secret generic fcm-credentials -n qa \
  --from-file=credentials.json=<caminho-do-json-da-service-account>

kubectl create secret generic fcm-credentials -n production \
  --from-file=credentials.json=<caminho-do-json-da-service-account>
```

Recomendado usar **um projeto Firebase para dev/qa e outro para produção**, para não misturar
push de teste com o de usuário real. Rotação: gere uma nova chave no Console do Firebase, revogue
a antiga, atualize o Secret e faça `kubectl rollout restart`.

## 5. `resend` (recuperação de senha)

`POST /api/v1/auth/password-reset/request` envia o e-mail via [Resend](https://resend.com). Ao
contrário das integrações acima, esta **não tem fallback**: sem o Secret, o pod sobe (a chave e o
remetente são `optional: true` por padrão vazio), mas a chamada ao Resend falha para todo pedido
de recuperação — só é logada, nunca derruba o fluxo (ver `ResendMailGateway`), então o sintoma é
"ninguém recebe o e-mail", sem erro visível na API.

```sh
kubectl create secret generic resend -n qa \
  --from-literal=api-key='<chave-da-api-do-resend>' \
  --from-literal=from-email='<remetente-verificado@dominio>'

kubectl create secret generic resend -n production \
  --from-literal=api-key='<chave-da-api-do-resend>' \
  --from-literal=from-email='<remetente-verificado@dominio>'
```

`from-email` precisa ser um remetente de **domínio verificado** no Resend (ou `onboarding@resend.dev`
só para teste). O link enviado no e-mail é um deep link (`zera://reset-password?token=...`), não
uma URL http — a tela de redefinição existe só no app mobile; ver `zera.mail.reset.deep-link-base`
em `application.properties` se o esquema do app mudar.

## 6. `legacy-db` (obrigatório em QA — sync com o banco do sistema legado)

Só o deployment de **QA** liga o sync (`LEGACY_SYNC_ENABLED=true`); produção segue desligada até o
sync ser validado em QA. O job lê organização, unidade e gestor do banco legado a cada 1 minuto
(`LEGACY_SYNC_INTERVAL`, padrão 1 minuto) e grava no banco do serviço; regras e limites em
`docs/legacy-db-field-mapping.md`. **Crie o Secret antes do deploy de QA.** Ao contrário dos outros
Secrets de integração, as chaves não são `optional`: com o sync ligado e a URL vazia a aplicação
nem sobe (`jdbcUrl is required`), então o pod novo fica em `CreateContainerConfigError`, o rollout
estoura o timeout e o pod antigo segue atendendo. Legado fora do ar, por outro lado, não impede a
subida (só gera `Legacy sync falhou` no log a cada rodada).

```sh
kubectl create secret generic legacy-db -n qa \
  --from-literal=url='jdbc:postgresql://<host>:5432/<banco>?sslmode=require' \
  --from-literal=user='<usuario-somente-leitura>' \
  --from-literal=password='<senha>'
```

O pod precisa alcançar o host do legado (libere o IP de saída do cluster no firewall dele). O
serviço só **lê** o legado, então use um usuário com `SELECT` em `organizacao`, `unidade`, `gestor`,
`assinatura` e `plano`. Depois de criar ou trocar o Secret: `kubectl rollout restart
deployment/ms-administrative-core -n qa`. Para conferir:
`kubectl logs -n qa deploy/ms-administrative-core | grep "Legacy sync"`.
