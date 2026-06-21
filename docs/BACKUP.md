# Respaldo y restauración de la base de datos

La base de datos (PostgreSQL en Render) se respalda con **`pg_dump`** (respaldo lógico,
formato comprimido) mediante los scripts de `scripts/`. Los dumps quedan en `backups/`
(carpeta **gitignored**: nunca se suben al repo porque contienen datos reales).

> El plan gratuito de Render **no** incluye respaldos automáticos, por eso usamos `pg_dump` manual/programado.

## Requisitos
- Cliente de PostgreSQL **>= versión del servidor** (Render corre **PostgreSQL 18**), porque `pg_dump`
  exige ser igual o más nuevo que el servidor. En macOS: `brew install postgresql@18`.
  (Los scripts usan automáticamente el cliente más nuevo instalado en `/opt/homebrew/opt/postgresql@XX`.)
- La conexión en la variable `DB_URL` (en un archivo `.env` local, **no versionado**):

  1. Copia el ejemplo: `cp .env.example .env`
  2. Edita `.env` y pon tu cadena real:
     ```
     DB_URL=postgresql://USUARIO:PASSWORD@HOST:5432/BASEDATOS?sslmode=require
     ```

## Crear un respaldo
```bash
make backup          # o:  npm run backup   o:  ./scripts/backup.sh
```
Genera `backups/facturacion_YYYYMMDD_HHMMSS.dump`. Los respaldos de más de **30 días**
se eliminan automáticamente.

## Restaurar un respaldo
```bash
make restore f=backups/facturacion_20260620_184500.dump
# o:  ./scripts/restore.sh backups/facturacion_20260620_184500.dump
```
Pide confirmación (escribir `si`) antes de sobrescribir, y usa
`pg_restore --clean --if-exists` (reemplaza los objetos existentes).

## Programarlo (opcional)
Respaldo diario a las 03:00 con cron (`crontab -e`):
```cron
0 3 * * * cd /ruta/al/proyecto && /bin/bash scripts/backup.sh >> backups/backup.log 2>&1
```

## Notas
- Formato **custom** (`pg_dump -Fc`): comprimido y permite restauración selectiva con `pg_restore`.
- Para un dump en SQL plano legible: `pg_dump "$DB_URL" --no-owner -f backup.sql` y restaurar con `psql "$DB_URL" -f backup.sql`.
- `DB_URL` es la misma variable que conviene usar para sacar los secretos de `application.properties` (config segura).
