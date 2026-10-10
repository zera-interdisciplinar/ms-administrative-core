# Contrato de API — Fluxo de descarte (`/api/v1/recyclings`, `/api/v1/recycling-places`, `/api/v1/telephone/recyclings`)

Documentação de contrato das rotas do `ms-administrative-core` que o app usa **antes e depois** do descarte físico: escolher **para onde** o material vai (parceira cadastrada ou ponto próximo) e manter o **contato** da recicladora.

**O que este microserviço não faz:** registrar o lote/item descartado, gerar comprovante, confirmar o descarte nem gravar `batch_id`. Isso é domínio do **ms-inventory**. A tabela `disposal_report` existe no Postgres deste serviço (migração inicial), mas **não há rota HTTP** nem entidade JPA para ela na API atual.

**Não confundir os dois catálogos:**

| Recurso | O que é | Persistido? |
|---|---|---|
| `/api/v1/recyclings` | Empresa recicladora **parceira** (CNPJ, e-mail, `placeId` opcional) | Sim — tabela `recycling_business` |
| `/api/v1/recycling-places` | Ponto **descoberto no Google** perto de `(lat, lng)` | Nome/endereço/horário: **não** (cache). Se o pin já foi vinculado, vêm `recyclingBusinessId` e `email` da ficha |

Contrato completo do mapa: [`recycling-places-api-contract.md`](recycling-places-api-contract.md). Alerta de reciclável no aterro: [`alerts-api-contract.md`](alerts-api-contract.md) (`kind` = `RECYCLABLE_TO_LANDFILL`).

---

## Papel deste serviço no fluxo (app)

1. Operador/gestor escolhe destino no mapa: `GET /api/v1/recycling-places?lat=&lng=`.
   - pin **sem vínculo** → só dados do Google (`recyclingBusinessId`/`email` nulos);
   - pin **já vinculado** → o mesmo item traz `recyclingBusinessId` e `email` da parceira; telefone continua em `GET /telephone/recyclings?recyclingBusinessId=`.
2. Alternativa: lista de fichas `GET /api/v1/recyclings` (e detalhe/`cnpj` se precisar). `placeId` nulo = parceira ainda sem ponto no mapa.
3. Gestor cadastra a ficha (`POST /recyclings` + telefone) e **liga** ao pin escolhido: `PATCH /recyclings/{id}/place-id?placeId=`.
4. O **evento de descarte do item** (aterro vs reciclagem, quantidade, comprovante) **não** passa por estas rotas. Se o inventory detectar reciclável no aterro, ele posta alerta aqui (`POST /notifications/alerts`).

Endereço da recicladora: a tabela `address` tem `recycling_business_id`, mas **não há controller de endereço**. Não dá para gravar/ler endereço via API hoje.

---

## Autenticação e headers comuns

| Header | Obrigatório | Descrição |
|---|---|---|
| `Authorization` | Sim | `Bearer <access_token>` — JWT de login (`POST /api/v1/auth/login`) ou refresh. |
| `Content-Type` | Sim (corpo JSON) | `application/json` |

Token ausente/inválido: `401`.

Este microserviço **não** valida header `apiKey`. Se a chamada passar pelo gateway, o edge pode exigir `x-api-key` à parte.

A unidade **não** vem de header nestas rotas. Recicladora cadastrada **não** é filtrada por `unitId`/`organizationId`: a lista `GET /recyclings` é **global** no banco deste serviço.

| Autorização | Rotas (neste contrato) |
|---|---|
| Autenticado (qualquer role, inclusive token de serviço que autentique) | `GET /recyclings`, `GET /recyclings/{id}`, `GET /recyclings/cnpj/{cnpj}`, `GET /recycling-places`, `GET /telephone/recyclings` |
| `MANAGER` | `POST /recyclings`, `PATCH /recyclings/{id}/name`, `PATCH /recyclings/{id}/email`, `PATCH /recyclings/{id}/place-id`, `POST /telephone/recyclings`, `PATCH /telephone/{id}/number`, `DELETE /telephone/{id}` |

Ator autenticado sem papel de gestor nas escritas: `403` (`"Acesso negado"` em ProblemDetail).

Não há envelope de paginação em `GET /recyclings`: a resposta é um **array JSON** (`List<RecyclingBusiness>`). `GET /recycling-places` também é array. Telefone da recicladora é **um** objeto, não lista.

---

## Shape compartilhado — `RecyclingBusiness`

Usado em `POST` (201), `GET` lista e `GET` por id/CNPJ. Campos `cnpj` e `email` serializam como **string** (`@JsonValue`), não como objeto.

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Cooperativa Recicla SP",
  "cnpj": "11222333000181",
  "email": "contato@reciclasp.com",
  "placeId": "places/ChIJxxxxxxxx",
  "createdAt": "2026-07-01T09:00:00",
  "updatedAt": "2026-08-01T10:00:00"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | Identificador da recicladora. É o `recyclingBusinessId` nas rotas de telefone e no pin do mapa. |
| `name` | string | Nome comercial. Coluna `name` VARCHAR(100). |
| `cnpj` | string | **14 dígitos**, sem máscara. Entrada aceita pontuação (`11.222.333/0001-81`); a API devolve só dígitos. |
| `email` | string | E-mail de contato, gravado em `contact_email`, normalizado (trim + minúsculas). |
| `placeId` | string ou `null` | `place_id` do Google Places ligado a esta ficha. `null` até o gestor chamar `PATCH .../place-id`. Pelos Termos do Google, **este** identificador pode ser persistido; nome/endereço do pin, não. |
| `createdAt` / `updatedAt` | datetime | `LocalDateTime` sem timezone. |

**Não** vêm neste JSON: telefone, endereço, `unitId`. Telefone continua em `/telephone/recyclings`.

---

## 1. `GET /api/v1/recyclings` — listar recicladoras cadastradas

Lista completa do cadastro interno. Sem query params, sem filtro por unidade, sem paginação.

### Request

```
GET /api/v1/recyclings HTTP/1.1
Authorization: Bearer <token>
```

```bash
curl -X GET "https://<host>/api/v1/recyclings" \
  -H "Authorization: Bearer eyJhbGciOi..."
```

### Response — `200 OK`

Array (pode ser vazio). Cada item: shape `RecyclingBusiness` acima.

| Status | Quando |
|---|---|
| `401` | Sem JWT ou token inválido. |

---

## 2. `GET /api/v1/recyclings/{id}` — detalhe por id

### Request

```
GET /api/v1/recyclings/3fa85f64-5717-4562-b3fc-2c963f66afa6 HTTP/1.1
Authorization: Bearer <token>
```

### Response — `200 OK`

Um `RecyclingBusiness`.

| Status | Quando |
|---|---|
| `400` | `{id}` não é UUID — `detail` típico: `"Parametro 'id' com valor invalido"`. |
| `401` | Sem JWT ou token inválido. |
| `404` | Id inexistente — `"Recycling not found with id: <uuid>"`. |

---

## 3. `GET /api/v1/recyclings/cnpj/{cnpj}` — detalhe por CNPJ

O path `{cnpj}` passa pelo mesmo validador de CNPJ (dígitos verificadores). Dá para mandar com ou sem máscara; o backend normaliza.

### Request

```
GET /api/v1/recyclings/cnpj/11222333000181 HTTP/1.1
Authorization: Bearer <token>
```

### Response — `200 OK`

Um `RecyclingBusiness`. `cnpj` na resposta continua só com 14 dígitos.

| Status | Quando |
|---|---|
| `400` | CNPJ inválido — `"Invalid CNPJ: <valor>"`. |
| `401` | Sem JWT ou token inválido. |
| `404` | CNPJ válido mas não cadastrado — `"Recycling not found with value: <cnpj da URL>"`. |

---

## 4. `POST /api/v1/recyclings` — cadastrar recicladora parceira

Rota de gestor. Cria o registro; **não** cria telefone nem endereço no mesmo request.

### Request

```
POST /api/v1/recyclings HTTP/1.1
Authorization: Bearer <token de MANAGER>
Content-Type: application/json

{
  "name": "Cooperativa Recicla SP",
  "cnpj": "11.222.333/0001-81",
  "email": "contato@reciclasp.com"
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `name` | string | Sim (na prática) | Nome. Sem `@NotBlank` no DTO; string nula/vazia ainda grava se o banco aceitar. |
| `cnpj` | string | Sim | CNPJ válido (máscara opcional). Inválido → `400`. |
| `email` | string | Sim | E-mail válido. Inválido → `400` (`"Invalid email: ..."`). |

Não há Bean Validation neste body: CNPJ/e-mail inválidos caem nas exceções de domínio, não em `field: must not be blank`.

```bash
curl -X POST "https://<host>/api/v1/recyclings" \
  -H "Authorization: Bearer eyJhbGciOi..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Cooperativa Recicla SP","cnpj":"11.222.333/0001-81","email":"contato@reciclasp.com"}'
```

### Response — `201 Created`

Corpo: `RecyclingBusiness` criado (`id` gerado no servidor).

| Status | Quando |
|---|---|
| `400` | JSON ilegível, CNPJ inválido ou e-mail inválido. |
| `401` | Sem JWT ou token inválido. |
| `403` | Token de `EMPLOYEE` (ou sem `ROLE_MANAGER`). |
| `409` | **Não mapeado neste use case.** CNPJ duplicado tem `UNIQUE` no banco (`recycling_business_cnpj_unique`); o cadastro de **organização** devolve `409` via `CnpjAlreadyInUseException`, o de recicladora **não** checa antes do `save`. Em Postgres real a duplicata tende a estourar no persist (não use `409` como contrato estável aqui). |

Não há `DELETE /recyclings/{id}` na API, embora o repositório tenha `delete`.

---

## 5. `PATCH /api/v1/recyclings/{id}/name` — renomear

Query param, **não** body JSON. Gestor. Resposta sem corpo.

### Request

```
PATCH /api/v1/recyclings/{id}/name?name=Novo%20Nome HTTP/1.1
Authorization: Bearer <token de MANAGER>
```

| Query param | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `name` | string | Sim | Novo nome. |

```bash
curl -X PATCH "https://<host>/api/v1/recyclings/<id>/name?name=Novo%20Nome" \
  -H "Authorization: Bearer eyJhbGciOi..."
```

### Response — `204 No Content`

| Status | Quando |
|---|---|
| `401` / `403` | Auth / papel. |
| `404` | Id inexistente — `"Recycling not found with id: <uuid>"`. |

---

## 6. `PATCH /api/v1/recyclings/{id}/email` — trocar e-mail de contato

Query param `email`. Gestor. `204` sem corpo.

### Request

```
PATCH /api/v1/recyclings/{id}/email?email=novo@reciclasp.com HTTP/1.1
Authorization: Bearer <token de MANAGER>
```

| Status | Quando |
|---|---|
| `400` | E-mail inválido. |
| `401` / `403` | Auth / papel. |
| `404` | Id inexistente. |

Não há unicidade de e-mail entre recicladoras (CNPJ unique; `place_id` unique quando preenchido).

---

## 7. `PATCH /api/v1/recyclings/{id}/place-id` — vincular pin do Google à ficha

Query param, **não** body. Gestor. É o que o mapa usa depois para devolver `recyclingBusinessId` e `email` no pin.

Um `placeId` só pode estar em **uma** parceira (índice único parcial `ux_recycling_business_place_id`). Religar o **mesmo** ponto à **mesma** ficha é aceito (`204`). Ligar a outra ficha: `409`.

`POST /recyclings` **não** aceita `placeId` no body — cadastro nasce desvinculado; o vínculo é este PATCH.

### Request

```
PATCH /api/v1/recyclings/{id}/place-id?placeId=places%2FChIJxxxxxxxx HTTP/1.1
Authorization: Bearer <token de MANAGER>
```

| Query param | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `placeId` | string | Sim | Valor de `placeId` vindo de `GET /recycling-places`. Máx. 200 caracteres na coluna. A API **não** valida se o id existe no Google. |

```bash
curl -X PATCH "https://<host>/api/v1/recyclings/<id>/place-id?placeId=places/ChIJxxxxxxxx" \
  -H "Authorization: Bearer eyJhbGciOi..."
```

### Response — `204 No Content`

| Status | Quando |
|---|---|
| `401` / `403` | Auth / papel. |
| `404` | Ficha inexistente — `"Recycling not found with id: <uuid>"`. |
| `409` | Esse `placeId` já está em **outra** recicladora — `"Recycling place already linked to another recycling business: <placeId>"`. |

Não há rota para **desvincular** (`place_id` de volta a `NULL`).

---

## 8. Telefone da recicladora (`/api/v1/telephone/recyclings`)

Um telefone por recicladora. Segundo cadastro no mesmo `recyclingBusinessId`: `409`.

Número: 10 ou 11 dígitos (DDD + fixo/celular). Máscara no input é aceita; a API devolve **só dígitos**.

### 8.1 `GET /api/v1/telephone/recyclings?recyclingBusinessId=`

Autenticado. Devolve **um** `TelephoneOutput`, não array.

```
GET /api/v1/telephone/recyclings?recyclingBusinessId=3fa85f64-5717-4562-b3fc-2c963f66afa6 HTTP/1.1
Authorization: Bearer <token>
```

```json
{
  "telephoneId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "number": "11987654321",
  "userId": null,
  "organizationId": null,
  "unitId": null,
  "recyclingBusinessId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "createdAt": "2026-07-01T09:00:00",
  "updatedAt": "2026-07-01T09:00:00"
}
```

`number` serializa como string. Os ids de usuário/org/unidade vêm `null` neste vínculo.

| Status | Quando |
|---|---|
| `400` | `recyclingBusinessId` ausente ou não-UUID. |
| `401` | Sem JWT. |
| `404` | Sem telefone para essa recicladora (a mensagem usa o UUID buscado). |

### 8.2 `POST /api/v1/telephone/recyclings` — cadastrar telefone

Gestor. A recicladora precisa existir.

```
POST /api/v1/telephone/recyclings HTTP/1.1
Authorization: Bearer <token de MANAGER>
Content-Type: application/json

{
  "recyclingBusinessId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "number": "(11) 98765-4321"
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `recyclingBusinessId` | UUID | Sim | Recicladora já criada em `POST /recyclings`. |
| `number` | string | Sim | Não branco; 10 ou 11 dígitos após normalizar. |

### Response — `201 Created`

Header `Location: /api/v1/telephone/{telephoneId}`.

```json
{
  "telephoneId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "number": "11987654321"
}
```

| Status | Quando |
|---|---|
| `400` | Body inválido (`recyclingBusinessId` nulo, `number` em branco) ou número com quantidade de dígitos errada — `"Invalid telephone number: ..."`. |
| `401` / `403` | Auth / papel. |
| `404` | Recicladora inexistente. |
| `409` | Já existe telefone para essa recicladora — `"Telephone already registered for recycling business: <uuid>"`. |

### 8.3 `PATCH /api/v1/telephone/{id}/number` e `DELETE /api/v1/telephone/{id}`

Mesmas rotas genéricas de telefone (gestor). `{id}` é o **`telephoneId`**, não o da recicladora.

`PATCH` usa **body** `{ "number": "..." }` (diferente dos PATCH de recicladora, que usam query). `204` nos dois.

Contrato mais amplo de telefone (usuário/org/unidade): [`team-management-api-contract.md`](team-management-api-contract.md).

---

## 9. `GET /api/v1/recycling-places` — pontos próximos (mapa do descarte)

Proxy Google Places. Depois do GET, o backend **cruza** cada `placeId` com `recycling_business.place_id` e preenche vínculo quando existir. Campos extras do Google neste pull: `isOpen`, `description`, `openingHours` (horários pedidos em `pt-BR`). Detalhe de raios/`503`: [`recycling-places-api-contract.md`](recycling-places-api-contract.md).

Busca interna: só **searchText** (`ferro velho`, `cooperativa de reciclagem`, `ecoponto`, `descarte eletronico`). Não há mais `searchNearby` por tipo `recycling_center` (a Places API New recusa esse tipo com 400).

No fluxo de descarte, o app costuma:

1. GPS / centro do mapa → `lat`/`lng`.
2. `GET /recycling-places?lat=&lng=` (opcional `radiusMeters`, teto 20 km).
3. `200` + lista → pins ordenados por `distanceMeters`;
   - `recyclingBusinessId` preenchido → parceira; e-mail no próprio pin; telefone via `/telephone/recyclings`;
   - ambos nulos → só Google; gestor ainda pode cadastrar ficha e `PATCH .../place-id`.
4. `200` + `[]` → nenhum ponto no raio; `503` → busca **não feita** — **não** tratar como “não existe recicladora”.

---

## Alerta ligado ao descarte (`RECYCLABLE_TO_LANDFILL`)

Quem **grava** o descarte (ms-inventory) pode notificar o gestor quando material reciclável vai para aterro:

```
POST /api/v1/notifications/alerts
Authorization: Bearer <token de serviço com notifications:write>
```

`kind`: `RECYCLABLE_TO_LANDFILL`. Body, deduplicação e catálogo: [`alerts-api-contract.md`](alerts-api-contract.md).

O usuário autenticado lista os próprios alertas em `GET /api/v1/notifications/alerts` (filtro pelo `sub` do JWT). Útil na tela de notificações depois do descarte; **não** substitui o relatório de descarte.

---

## O que ainda não é API (schema vs HTTP)

| Objeto no banco | API hoje |
|---|---|
| `recycling_business` | CRUD parcial: create, list, get, patch name/email/`place-id`. Sem delete nem unlink. |
| `telephone.recycling_business_id` | GET/POST em `/telephone/recyclings`; PATCH/DELETE pelo `telephoneId`. |
| `address.recycling_business_id` | Sem rotas. |
| `disposal_report` | Sem rotas. Não usar como contrato do app. |

---

## Qual rota escolher

| Preciso de... | Rota |
|---|---|
| Lista de **parceiras cadastradas** (CNPJ) | `GET /recyclings` |
| Ficha de uma parceira (inclui `placeId` se já vinculado) | `GET /recyclings/{id}` ou `GET /recyclings/cnpj/{cnpj}` |
| Cadastrar / corrigir nome ou e-mail | `POST /recyclings`, `PATCH .../name`, `PATCH .../email` (`MANAGER`) |
| Ligar a ficha ao pin do mapa | `PATCH /recyclings/{id}/place-id?placeId=` (`MANAGER`) |
| Telefone para ligar para a parceira | `GET /telephone/recyclings?recyclingBusinessId=` |
| Incluir telefone na ficha | `POST /telephone/recyclings` (`MANAGER`) |
| Pins “perto de mim” + e-mail se já vinculado | `GET /recycling-places?lat=&lng=` |
| Saber se o mapa falhou vs. zero resultados | `503` vs `200 []` — contrato de recycling-places |
| Notificar reciclável no aterro | `POST /notifications/alerts` com `kind=RECYCLABLE_TO_LANDFILL` (serviço) |
| Registrar o descarte do **item/lote** | **Não neste serviço** — ms-inventory |
| Comprovante / confirmação (`disposal_report`) | **Não exposto** nesta API |
