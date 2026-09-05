# k8s — ms-administrative-core

Manifests aplicados pelos workflows `deploy-qa` (namespace `qa`) e `deploy-prod`
(namespace `production`). Além dos manifests versionados aqui, cada ambiente
precisa de dois Secrets criados manualmente (uma vez).

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

## 2. `ms-administrative-core-bootstrap` (opcional)

Consumido **apenas** pela migration `V8`, que cria a árvore
`organization → unit → MANAGER` inicial. Sem este Secret a migration é no-op.
É idempotente pelo email: rodar de novo com um email já existente não faz nada.

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
