# Facturación DTE — Backend

Backend de facturación electrónica (DTE chileno) en **Spring Boot 4 / Java 21 / PostgreSQL**.
Incluye API REST, **panel de administración**, **Swagger** y scripts de **respaldo** de la BD.

## Requisitos
- Java 21
- (Para respaldos) cliente PostgreSQL 18 — `brew install postgresql@18`
- (Opcional) Node/npm o `make` para los atajos

## Cómo correr (local)
```bash
make dev          # o:  npm run dev   o:  ./mvnw spring-boot:run
```
Cuando aparezca `Started FacturacionBackendApplication`:

| URL | Qué es |
|-----|--------|
| http://localhost:8080 | **Swagger** (endpoints + pruebas, modo oscuro) |
| http://localhost:8080/admin | **Panel de administración** (login) |

**Acceso al panel:** solo `ROLE_SUPER_ADMIN`. Este rol es global y no se asocia a
ninguna empresa. Usuario sembrado por defecto: **`root` / `1234`**.

### Empaquetar / correr el .jar
```bash
make build        # genera target/facturacion-backend-0.0.1-SNAPSHOT.jar
make jar          # empaqueta y lo ejecuta
```

## Datos iniciales (seed automático)
Al arrancar, `DataSeeder` crea (si faltan): roles `ROLE_SUPER_ADMIN/ADMIN/USER`,
el super-usuario `root/1234`, y los tipos de documento 33/34/52/56/61.

## Seguridad por roles
- Público: `/auth/**`, `POST /empresas` (bootstrap), `/api/tipos-documento`, panel y Swagger.
- Solo admin/super-admin: `/usuarios`, `/empresas`, `/roles`, `/auditoria`, `/api/folios`.
- Cualquier usuario autenticado: clientes, productos, documentos.

## Auditoría
Las operaciones de escritura de los servicios se registran en la tabla `auditoria`
(quién, qué tabla, qué acción, cuándo). Visible en el panel → **Sistema → Auditoría**
o en `GET /auditoria`.

## Importar documentos desde TXT
- Por el panel: **Acciones → Importar TXT** (previsualizar y crear BORRADOR).
- Por API: `POST /api/documentos/importar-txt/preview` y `POST /api/documentos/importar-txt`.

El tipo se deduce del prefijo del N° de documento (`FA`→33, `FE`→34, `NC`→61, `ND`→56);
la moneda se detecta de las líneas (USD/EUR exento de IVA, CLP afecto).

## Respaldos de la base de datos
Ver [`docs/BACKUP.md`](docs/BACKUP.md). Resumen:
```bash
make backup                          # genera backups/facturacion_*.dump
make restore f=backups/archivo.dump  # restaura
```
Requiere `DB_URL` en un archivo `.env` (copia `.env.example`).

## Despliegue (Docker / Render)
```bash
docker build -t facturacion-dte .
docker run -p 8080:8080 facturacion-dte
```
En Render: Web Service tipo Docker (ver `render.yaml`). Para apuntar a otra BD u ocultar
secretos, define `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `SRE_API_TOKEN` como variables de entorno.
