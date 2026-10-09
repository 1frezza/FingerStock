#!/usr/bin/env bash
# Dump lógico do banco (RDS) para a EC2 e, opcionalmente, para um bucket S3.
# Complementa os backups automáticos (snapshots) do próprio RDS.
# Instalado pelo setup-ec2.sh em /usr/local/bin/fingerstock-backup e agendado no cron.
set -euo pipefail

set -a
source /etc/fingerstock/app.env
set +a

DESTINO=/var/backups/fingerstock
DIAS_RETENCAO=7
ARQUIVO="$DESTINO/${DB_NAME}_$(date +%Y-%m-%d_%H%M%S).sql.gz"

mkdir -p "$DESTINO"

MYSQL_PWD="$DB_PASSWORD" mysqldump \
    --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" \
    --single-transaction --no-tablespaces --routines \
    "$DB_NAME" | gzip > "$ARQUIVO"

echo "$(date '+%F %T') backup gerado: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"

# Requer uma IAM Role na EC2 com permissão s3:PutObject no bucket
if [[ -n "${BACKUP_S3_BUCKET:-}" ]]; then
    aws s3 cp "$ARQUIVO" "s3://$BACKUP_S3_BUCKET/fingerstock/$(basename "$ARQUIVO")" --only-show-errors
    echo "$(date '+%F %T') enviado para s3://$BACKUP_S3_BUCKET/fingerstock/"
fi

find "$DESTINO" -name '*.sql.gz' -mtime +"$DIAS_RETENCAO" -delete
