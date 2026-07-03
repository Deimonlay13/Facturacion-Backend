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

## Logo de empresa
- Subir logo: `POST /empresas/{id}/logo` usando `multipart/form-data` con el campo `file`.
- Ver logo: `GET /empresas/{id}/logo`.
- Eliminar logo: `DELETE /empresas/{id}/logo`.
- Formatos permitidos: PNG o JPG/JPEG, máximo 1 MB.
- Si la empresa tiene logo cargado, se imprime automáticamente en el PDF de la factura.

## Flujo simple de folios
1. Cargar folios simples: `POST /api/folios/simple` con `codigoTipoDocumento` y `cantidad`.
   Ejemplo: si cargas 20 folios para factura afecta `33`, se crean los folios 1 al 20.
   Si después cargas 30 más, se crean automáticamente del 21 al 50.
   Aplica para factura afecta `33`, factura exenta `34`, nota de débito `56` y nota de crédito `61`.
   También existe la carga CAF/rango tradicional: `POST /api/folios/caf`.
2. Revisar disponibilidad: `GET /api/folios/resumen` o
   `GET /api/folios/resumen/{codigoTipoDocumento}`. La respuesta muestra el CAF activo,
   el siguiente folio, los disponibles y si está listo para emitir.
3. Crear documento en borrador.
4. Emitir documento: el backend toma automáticamente el siguiente folio disponible,
   lo marca como utilizado y deja el documento en estado `EMITIDO`.
5. Si no hay folios disponibles, el backend rechaza la emisión y no permite facturar.
6. Descargar PDF: si el documento fue emitido, saldrá con folio real; si no, saldrá como
   `BORRADOR`.

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
## Despliegue en AWS EC2 con Docker Compose
1. Provisiona una EC2 con Docker y Docker Compose instalados.
2. Copia el repositorio a la instancia y crea un archivo `.env` basado en `.env.example`.
3. Ajusta `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` y `SRE_API_TOKEN` según tu entorno.
4. Lanza los contenedores:
   ```bash
docker compose up -d
```
5. El backend quedará disponible en `http://<EC2-IP>:8080`.

> En AWS EC2, solo necesitas exponer el puerto 8080 en el Security Group. La base de datos PostgreSQL queda dentro del mismo `docker compose` y no debe abrirse públicamente.

## Modo local y dev con perfiles Spring
- `local`: corre tu app directamente en tu máquina con `application-local.properties`.
- `dev`: corre tu app en Docker Compose con `application-dev.properties`.

### Ejecutar local
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Ejecutar en Docker Compose (dev)
```bash
docker compose up -d
```

El archivo `docker-compose.yml` activa automáticamente el perfil `dev` para el contenedor de la app.

## Docker Compose para EC2
Se agrega un `docker-compose.yml` que levanta la aplicación Spring Boot junto a PostgreSQL en la misma EC2. Para cambiar credenciales, actualiza el archivo `.env` local y vuelve a recrear los servicios con `docker compose up -d`.
