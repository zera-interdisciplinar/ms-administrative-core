#!/usr/bin/env bash
#
# Backup logico do banco do ms-administrative-core.
#
# Formato `custom` (-Fc) e nao SQL puro: permite restauracao seletiva (uma tabela, um schema),
# paraleliza o restore com --jobs e ja vem comprimido. Restaurar so `user_account` a partir de um
# .sql de varios GB exigiria editar o arquivo na mao.
#
# Uso:
#   DB_HOST=... DB_NAME=... DB_USER=... DB_PASSWORD=... ./scripts/backup.sh
#
# Ver docs/backup-recuperacao.md para restauracao, PITR e ensaio de recuperacao.
set -euo pipefail

: "${DB_HOST:?defina DB_HOST}"
: "${DB_NAME:?defina DB_NAME}"
: "${DB_USER:?defina DB_USER}"
: "${DB_PASSWORD:?defina DB_PASSWORD}"

DESTINO="${BACKUP_DIR:-/var/backups/zera}"
RETENCAO_DIAS="${BACKUP_RETENTION_DAYS:-7}"
DATA="$(date -u +%Y%m%d-%H%M%S)"
ARQUIVO="$DESTINO/administrative-core-$DATA.dump"

mkdir -p "$DESTINO"

echo "[backup] iniciando dump de $DB_NAME em $DB_HOST"
PGPASSWORD="$DB_PASSWORD" pg_dump \
  --host="$DB_HOST" \
  --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --format=custom \
  --compress=9 \
  --file="$ARQUIVO"

# Verificacao NA HORA: um dump corrompido que ninguem abriu nao e backup, e um arquivo.
# `pg_restore --list` le o cabecalho e o indice de objetos, entao um truncamento aparece aqui.
echo "[backup] verificando integridade"
pg_restore --list "$ARQUIVO" > /dev/null

sha256sum "$ARQUIVO" > "$ARQUIVO.sha256"

echo "[backup] expurgando backups com mais de $RETENCAO_DIAS dias"
find "$DESTINO" -name 'administrative-core-*.dump'        -mtime "+$RETENCAO_DIAS" -delete
find "$DESTINO" -name 'administrative-core-*.dump.sha256' -mtime "+$RETENCAO_DIAS" -delete

echo "[backup] concluido: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"

# ATENCAO: as chaves RSA do JWT (JWT_PRIVATE_KEY/JWT_PUBLIC_KEY) NAO estao no banco e NAO entram
# neste dump. Restaurar o banco sem elas devolve os dados e ninguem consegue logar, porque nenhum
# outro servico valida os tokens emitidos com um par novo. Elas pertencem ao mesmo plano de
# recuperacao e precisam de backup proprio, no cofre de segredos.
