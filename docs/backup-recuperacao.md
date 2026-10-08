# Backup e recuperacao

Procedimentos documentados de backup, restauracao e resposta a falha do Postgres do
`ms-administrative-core`.

> **Este banco e o mais critico do sistema.** Ele emite os JWT que o `ms-inventory` e o AI core
> validam, e guarda a identidade de todos os usuarios. Perder o `ms-inventory` derruba um dominio;
> perder este derruba o **login de todo mundo**, e nenhum outro servico consegue reconstruir o que
> estava aqui.

## 1. Objetivos declarados

| Metrica | Alvo | Significa |
| --- | --- | --- |
| **RPO** (Recovery Point Objective) | 24 h com backup logico; 5 min com PITR | Quanto dado se aceita perder. |
| **RTO** (Recovery Time Objective) | 2 h | Quanto tempo se aceita ficar fora. |
| Retencao | 7 diarios, 4 semanais, 3 mensais | Janela para detectar corrupcao silenciosa. |

Alvo declarado nao e alvo atingido. **Ele so vale apos o primeiro ensaio de restauracao** (secao 6):
antes disso, RTO e uma estimativa, nao um compromisso.

## 2. O que precisa ser preservado

| Item | Onde | Se for perdido |
| --- | --- | --- |
| Dados (todas as tabelas) | Postgres | Irrecuperavel. |
| Objetos logicos (functions, procedures, triggers, views `bi`, roles) | Migracoes Flyway | Recuperavel: reaplicar as migracoes. |
| **Chaves RSA do JWT** | Secret do k8s (`JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`) | **Nao esta no banco.** Backup do banco sem backup do secret restaura os dados e ninguem consegue logar — os tokens emitidos param de validar em todos os servicos. |
| Segredos de cliente de servico (`MS_INVENTORY_CLIENT_SECRET`) | Secret do k8s | O `ms-inventory` perde acesso as rotas internas. |

**O backup do banco nao e suficiente por si so.** Os dois ultimos itens pertencem ao mesmo plano de
recuperacao e ficam fora do `pg_dump`.

## 3. Backup logico (`pg_dump`) — linha de base

Formato `custom` (`-Fc`), nao SQL puro: permite restauracao **seletiva** (uma tabela, um schema) e ja
vem comprimido. Restaurar so `user_account` a partir de um `.sql` de 4 GB exige editar o arquivo na
mao.

```bash
#!/usr/bin/env bash
# scripts/backup.sh — backup logico diario
set -euo pipefail

DATA=$(date +%Y%m%d-%H%M%S)
DESTINO="${BACKUP_DIR:-/var/backups/zera}"
ARQUIVO="$DESTINO/administrative-core-$DATA.dump"

mkdir -p "$DESTINO"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  --host="$DB_HOST" --username="$DB_USER" --dbname="$DB_NAME" \
  --format=custom --compress=9 --verbose \
  --file="$ARQUIVO"

# Integridade verificada NA HORA: um dump corrompido que ninguem abriu nao e backup,
# e um arquivo. pg_restore --list le o cabecalho e o indice de objetos.
pg_restore --list "$ARQUIVO" > /dev/null

sha256sum "$ARQUIVO" > "$ARQUIVO.sha256"

# Expurgo por retencao
find "$DESTINO" -name 'administrative-core-*.dump' -mtime +7 -delete
find "$DESTINO" -name 'administrative-core-*.dump.sha256' -mtime +7 -delete

echo "Backup concluido: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"
```

Agendamento: `CronJob` do k8s as 03:00 UTC, ou `cron` no host.

### Backup que exclui dado pessoal (homologacao)

Para carregar homologacao sem levar PII, o catalogo de dados diz exatamente o que excluir:

```sql
SELECT tabela, coluna FROM catalogo_coluna
 WHERE contem_pii = TRUE OR nivel_acesso = 'SECRETO'
 ORDER BY tabela, coluna;
```

```bash
# Nunca leve credencial para homologacao: hash de senha e de refresh token ficam fora.
pg_dump ... --exclude-table-data=refresh_token --exclude-table-data='audit_log*' \
            --file=homolog.dump
# depois do restore, anonimizar:
#   UPDATE user_account SET email = 'user' || id || '@exemplo.invalid',
#                           name = 'Usuario ' || left(id::text, 8),
#                           password = '$2a$10$<hash de senha descartavel>';
```

## 4. PITR (Point-In-Time Recovery) — para RPO de minutos

`pg_dump` diario significa aceitar perder ate 24 h. Para RPO de minutos e preciso arquivamento de
WAL.

### 4a. Postgres gerenciado (Neon, RDS, Cloud SQL, Supabase)

O provedor ja faz PITR. **O que precisa ser feito e documentado, nao implementado:**

1. Conferir no painel que o PITR esta **ligado** e qual a janela (7 dias e o comum).
2. Registrar aqui o procedimento de restauracao do provedor — normalmente "restore to point in time"
   cria uma **instancia nova**, com **endpoint novo**. A consequencia operacional e que restaurar
   exige trocar `DB_HOST` no secret e reiniciar os pods.
3. Manter o `pg_dump` diario **mesmo assim**: snapshot do provedor nao protege de erro de conta,
   exclusao de projeto ou fim de contrato. Backup que vive so no mesmo fornecedor que o banco tem um
   unico ponto de falha.

### 4b. Postgres autogerenciado

```ini
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /var/lib/postgresql/wal/%f && cp %p /var/lib/postgresql/wal/%f'
archive_timeout = 300          # forca fechar segmento a cada 5 min -> RPO de 5 min
```

Base backup semanal:

```bash
pg_basebackup --host="$DB_HOST" --username=replicador \
  --pgdata=/var/backups/zera/base-$(date +%Y%m%d) \
  --format=tar --gzip --wal-method=stream --checkpoint=fast --progress
```

Restauracao para um instante:

```bash
systemctl stop postgresql
rm -rf /var/lib/postgresql/16/main/*
tar -xzf /var/backups/zera/base-20260901/base.tar.gz -C /var/lib/postgresql/16/main/

cat >> /var/lib/postgresql/16/main/postgresql.conf <<'EOF'
restore_command = 'cp /var/lib/postgresql/wal/%f %p'
recovery_target_time = '2026-09-30 14:25:00'
recovery_target_action = 'promote'
EOF

touch /var/lib/postgresql/16/main/recovery.signal
systemctl start postgresql
# acompanhar em pg_wal e no log ate "database system is ready to accept connections"
```

## 5. Restauracao completa

```bash
# 1. Parar a aplicacao. Restaurar com a aplicacao escrevendo produz um estado que nao existiu.
kubectl scale deployment ms-administrative-core --replicas=0

# 2. Conferir o arquivo ANTES de destruir o que existe
sha256sum -c administrative-core-20260930-030000.dump.sha256
pg_restore --list administrative-core-20260930-030000.dump | head

# 3. Banco novo e vazio. Nao restaurar sobre um banco com dado:
#    o pg_restore falharia parcialmente e deixaria um hibrido dos dois estados.
createdb -h "$DB_HOST" -U "$DB_USER" zera_restore

# 4. Restaurar
pg_restore --host="$DB_HOST" --username="$DB_USER" --dbname=zera_restore \
  --jobs=4 --verbose administrative-core-20260930-030000.dump

# 5. Validar (secao 5a) ANTES de apontar a aplicacao

# 6. Promover e subir
psql -h "$DB_HOST" -U "$DB_USER" -d postgres \
  -c "ALTER DATABASE zera RENAME TO zera_quebrado;" \
  -c "ALTER DATABASE zera_restore RENAME TO zera;"
kubectl scale deployment ms-administrative-core --replicas=2
```

`--jobs=4` paraleliza a restauracao (so funciona com formato `custom`/`directory`) e e o que mantem o
RTO alcancavel num banco grande.

### 5a. Checklist de validacao pos-restauracao

Nao subir a aplicacao antes de todos os itens passarem.

```sql
-- 1. Versao do schema: o dump e da mesma versao de migracao que o codigo em producao?
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- 2. Objetos logicos vieram (pg_dump traz, mas conferir e barato)
SELECT count(*) FROM pg_proc WHERE proname LIKE 'fn_%' OR proname LIKE 'sp_%';   -- >= 10
SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal;                          -- >= 4
SELECT count(*) FROM pg_views WHERE schemaname = 'bi';                           -- 10
SELECT count(*) FROM pg_inherits;                                                -- 3

-- 3. Roles: sao objetos do CLUSTER e NAO vem no pg_dump de um banco.
--    Se estiverem faltando, reaplicar a V17.
SELECT rolname FROM pg_roles WHERE rolname IN ('zera_bi_leitor', 'zera_auditor');

-- 4. Catalogo x schema real: divergencia aqui indica restauracao incompleta
SELECT * FROM fn_catalogo_divergencia();                                          -- vazio

-- 5. Contagens contra o esperado do dia do backup
SELECT 'organization' t, count(*) FROM organization
UNION ALL SELECT 'unit', count(*) FROM unit
UNION ALL SELECT 'user_account', count(*) FROM user_account
UNION ALL SELECT 'alert', count(*) FROM alert;

-- 6. Integridade referencial: nenhum orfao
SELECT count(*) FROM user_account u LEFT JOIN unit un ON un.id = u.unit_id WHERE un.id IS NULL;   -- 0
SELECT count(*) FROM alert a LEFT JOIN unit un ON un.id = a.unit_id WHERE un.id IS NULL;          -- 0

-- 7. Estatisticas: sem isso a aplicacao volta lenta e parece que a restauracao falhou
ANALYZE;
```

Depois, pela API:

```bash
curl -s localhost:8080/actuator/health                      # UP
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"...","password":"..."}'                     # 200 e token valido
curl -s localhost:8080/.well-known/jwks.json                # chave publica esperada
```

O teste de login e o que importa mais: ele prova de uma vez que os dados voltaram **e** que as chaves
JWT do secret continuam casando com o que os outros servicos esperam.

## 6. Ensaio de restauracao (trimestral)

**Backup nao testado nao e backup.** O ensaio e o unico jeito de descobrir que o dump esta corrompido,
que o procedimento tem um passo errado ou que o RTO real e o dobro do declarado — e e muito melhor
descobrir isso numa terca-feira calma.

| # | Passo | Registrar |
| --- | --- | --- |
| 1 | Escolher um backup **sem avisar** qual sera | data do arquivo |
| 2 | Restaurar em ambiente isolado (container local serve) | inicio / fim |
| 3 | Rodar o checklist 5a inteiro | itens que falharam |
| 4 | Subir a aplicacao contra o banco restaurado | sim/nao |
| 5 | Fazer login com um usuario real | sim/nao |
| 6 | Medir o tempo total | **RTO real** |
| 7 | Anotar toda divergencia entre este documento e o que foi feito | e corrigir o documento |

Ensaio local, sem infraestrutura nenhuma:

```bash
docker run -d --name zera-restore-test -e POSTGRES_PASSWORD=x -p 55433:5432 postgres:16-alpine
docker cp administrative-core-*.dump zera-restore-test:/tmp/backup.dump
docker exec zera-restore-test createdb -U postgres zera
docker exec zera-restore-test pg_restore -U postgres -d zera --jobs=4 /tmp/backup.dump
docker exec -i zera-restore-test psql -U postgres -d zera -c "SELECT * FROM fn_catalogo_divergencia();"
```

### Registro dos ensaios

| Data | Backup usado | RTO real | Itens falhados | Responsavel |
| --- | --- | --- | --- | --- |
| _(preencher no primeiro ensaio)_ | | | | |

## 7. Respostas a falha

### Instancia perdida / banco nao sobe

1. Ver se e o banco ou o disco: `pg_isready`, espaco em disco (`df -h`), log do Postgres.
2. Disco cheio e a causa mais comum e nao precisa de restauracao: liberar espaco (WAL antigo,
   `audit_log_*` de filha antiga) e reiniciar.
3. Se o dado esta corrompido, seguir a secao 5.

### Corrupcao silenciosa (dado errado, sem erro)

O motivo de reter 7 dias em vez de sobrescrever um unico arquivo: corrupcao logica so aparece dias
depois, e um backup de ontem ja estaria contaminado. Use a auditoria para achar **quando** comecou:

```sql
SELECT ocorrido_em, operacao, usuario_banco, usuario_app, dados_antigos, dados_novos
  FROM audit_log_user_account
 WHERE registro_id = '<id suspeito>'
 ORDER BY ocorrido_em;
```

Com a data, restaurar por PITR para o instante **anterior** a primeira linha ruim.

### Exclusao acidental de linha

A auditoria guarda `dados_antigos` em JSONB, o que permite reconstruir a linha sem restaurar o banco:

```sql
SELECT dados_antigos FROM audit_log_user_account
 WHERE operacao = 'DELETE' AND registro_id = '<id>'
 ORDER BY id DESC LIMIT 1;
```

**Limite conhecido:** `password` e mascarado no log, entao a senha nao volta — o usuario restaurado
precisa de troca de senha. Isso e deliberado: guardar o hash no log seria vazamento. Registrado aqui
para que ninguem descubra isso no meio de um incidente.

### Sessoes apos restauracao

`refresh_token` restaurado de um backup de ontem contem tokens que **ja foram usados** desde entao.
Nao ha rastreamento de reuso, entao a decisao segura apos restauracao com perda de dados e invalidar
todas as sessoes:

```sql
UPDATE refresh_token SET revoked = TRUE;
```

Todos reautenticam. Access token continua valido ate 15 min (TTL configurado), o que da uma janela
curta de tolerancia.

## 8. Rotinas de manutencao relacionadas

As procedures da [`V14`](../src/main/resources/db/migration/V14__create_business_procedures.sql)
mantem o banco em tamanho saudavel, o que **e** parte de backup: banco menor restaura mais rapido.

| Procedure | Efeito | Acionamento |
| --- | --- | --- |
| `sp_revogar_tokens_expirados` | Revoga e expurga `refresh_token` vencido | `POST /api/v1/maintenance/tokens/revoke-expired` ou agendador |
| `sp_consolidar_dau` | Reconstroi o rollup de DAU | `POST /api/v1/maintenance/dau/consolidate` |
| `sp_fechar_alertas_obsoletos` | Fecha alertas OPEN antigos | Manual — **desligado por padrao**, `alert.status` e observado pelo `ms-inventory` |

Crescimento a acompanhar: `audit_log_*` cresce mais rapido que todas as tabelas de dominio somadas
(um INSERT de alerta gera uma linha de auditoria com a linha inteira em JSONB). A heranca existe para
que uma filha antiga possa ser arquivada ou truncada **sem travar a auditoria das outras**:

```sql
-- arquivar e liberar espaco de uma filha, sem tocar nas demais
CREATE TABLE audit_log_alert_2025 AS
  SELECT * FROM ONLY audit_log_alert WHERE ocorrido_em < DATE '2026-01-01';
DELETE FROM ONLY audit_log_alert WHERE ocorrido_em < DATE '2026-01-01';
-- exportar audit_log_alert_2025 para armazenamento frio antes de dropar
```

## 9. Pendencias reconhecidas

Este documento descreve o procedimento; estes itens ainda nao foram executados:

- [ ] Primeiro ensaio de restauracao (secao 6) — **sem ele o RTO de 2 h e estimativa**
- [ ] `scripts/backup.sh` agendado num `CronJob` do k8s
- [ ] Confirmar e registrar se o Postgres e gerenciado (4a) ou autogerenciado (4b)
- [ ] Backup dos secrets (chaves JWT) num cofre, com procedimento de recuperacao proprio
- [ ] Alerta para "backup nao rodou nas ultimas 26 h" — backup que falha em silencio e pior que
      nenhum, porque cria a confianca sem a protecao
