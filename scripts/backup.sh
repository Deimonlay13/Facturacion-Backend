#!/usr/bin/env bash
#
# Respaldo lógico de la base de datos (PostgreSQL / Render) con pg_dump.
# Genera un dump comprimido (formato custom) con timestamp en backups/.
#
# Uso:   ./scripts/backup.sh        (o:  make backup  /  npm run backup)
# Requiere la variable de entorno DB_URL (puede venir de un archivo .env local):
#   DB_URL="postgresql://usuario:password@host:5432/basedatos?sslmode=require"
#
set -euo pipefail
cd "$(dirname "$0")/.."

# Usa el cliente de PostgreSQL más nuevo disponible (pg_dump debe ser >= versión del servidor)
for v in 18 17 16; do
  if [ -x "/opt/homebrew/opt/postgresql@$v/bin/pg_dump" ]; then
    export PATH="/opt/homebrew/opt/postgresql@$v/bin:$PATH"; break
  fi
done

# Carga variables desde .env si existe (no versionado)
if [ -f .env ]; then set -a; . ./.env; set +a; fi

: "${DB_URL:?Falta DB_URL. Define la conexión, ej: export DB_URL='postgresql://user:pass@host:5432/db?sslmode=require' (o ponla en .env)}"

mkdir -p backups
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT="backups/facturacion_${STAMP}.dump"

echo "→ Generando respaldo en $OUT ..."
pg_dump "$DB_URL" --format=custom --no-owner --no-privileges --file="$OUT"
echo "✓ Respaldo listo: $OUT ($(du -h "$OUT" | cut -f1))"

# Retención: elimina respaldos de más de 30 días
find backups -name 'facturacion_*.dump' -mtime +30 -delete 2>/dev/null || true
