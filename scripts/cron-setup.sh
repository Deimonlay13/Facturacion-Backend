#!/usr/bin/env bash
#
# Programa un respaldo diario de la base de datos a las 03:00 usando cron.
# Idempotente: reemplaza la entrada anterior de backup.sh si ya existía.
#
# Uso:   ./scripts/cron-setup.sh        (o:  make backup-cron)
#
set -euo pipefail
cd "$(dirname "$0")/.."
PROJ="$(pwd)"
LINE="0 3 * * * cd $PROJ && /bin/bash scripts/backup.sh >> $PROJ/backups/backup.log 2>&1"

mkdir -p backups
( crontab -l 2>/dev/null | grep -v "scripts/backup.sh" ; echo "$LINE" ) | crontab -

echo "✓ Respaldo diario programado a las 03:00:"
echo "    $LINE"
echo "Ver tareas:   crontab -l"
echo "Editar/quitar: crontab -e"
