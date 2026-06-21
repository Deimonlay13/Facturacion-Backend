#!/usr/bin/env bash
#
# Restauración de la base de datos desde un dump generado por backup.sh (pg_restore).
#
# Uso:   ./scripts/restore.sh backups/facturacion_YYYYMMDD_HHMMSS.dump
#        (o:  make restore f=backups/archivo.dump)
# Requiere DB_URL (igual que backup.sh). ADVIERTE antes de sobrescribir.
#
set -euo pipefail
cd "$(dirname "$0")/.."

# Usa el cliente de PostgreSQL más nuevo disponible (debe ser >= versión del servidor)
for v in 18 17 16; do
  if [ -x "/opt/homebrew/opt/postgresql@$v/bin/pg_restore" ]; then
    export PATH="/opt/homebrew/opt/postgresql@$v/bin:$PATH"; break
  fi
done

if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${DB_URL:?Falta DB_URL (ponla en .env o expórtala)}"

FILE="${1:?Uso: ./scripts/restore.sh <archivo.dump>}"
[ -f "$FILE" ] || { echo "✗ No existe el archivo: $FILE"; exit 1; }

echo "⚠  Esto va a SOBRESCRIBIR objetos en la BD destino con el contenido de:"
echo "   $FILE"
read -r -p "   Escribe 'si' para continuar: " ok
[ "$ok" = "si" ] || { echo "Cancelado."; exit 0; }

echo "→ Restaurando ..."
pg_restore --clean --if-exists --no-owner --no-privileges --dbname="$DB_URL" "$FILE"
echo "✓ Restauración completa."
