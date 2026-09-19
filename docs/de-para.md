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