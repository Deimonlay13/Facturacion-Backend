#!/usr/bin/env bash
#
# Crea datos de PRUEBA en la empresa del usuario root (vía API):
# CAF (folios) para todos los tipos, clientes, productos y documentos (uno emitido).
# Requiere la app corriendo y el super-usuario root/1234.
#
# Uso:  ./scripts/seed-demo.sh   [BASE_URL]   (default http://localhost:8080)
#
set -euo pipefail
B="${1:-http://localhost:8080}"
JSON="Content-Type: application/json"

TOKEN=$(curl -s -X POST "$B/auth/login" -H "$JSON" -d '{"username":"root","password":"1234"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
AUTH="Authorization: Bearer $TOKEN"
echo "Token de root OK."

jid(){ python3 -c "import sys,json
try:
  d=json.load(sys.stdin); print(d.get('id') or (d.get('documento') or {}).get('id') or '')
except Exception: print('')"; }

echo "== CAF (folios 1-100 por tipo) =="
for T in 33 34 52 56 61; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$B/api/folios/caf" -H "$AUTH" -H "$JSON" \
    -d "{\"codigoTipoDocumento\":$T,\"rangoDesde\":1,\"rangoHasta\":100,\"fechaAutorizacion\":\"2026-06-20\",\"fechaVencimiento\":\"2027-06-20\",\"cafXml\":\"<CAF><TIPO>$T</TIPO></CAF>\"}")
  echo "  tipo $T -> HTTP $code"
done

cliente(){ # rut razon email
  local id
  id=$(curl -s -X POST "$B/clientes" -H "$AUTH" -H "$JSON" -d "{\"rut\":\"$1\",\"razonSocial\":\"$2\",\"email\":\"$3\"}" | jid)
  if [ -z "$id" ]; then
    id=$(curl -s "$B/clientes" -H "$AUTH" | python3 -c "import sys,json;d=json.load(sys.stdin);print(next((c['id'] for c in d if c['rut']=='$1'),''))")
  fi
  echo "$id"
}

echo "== Clientes =="
C1=$(cliente "22222222-2" "Comercializadora Andes SpA" "andes@demo.cl");      echo "  Andes -> $C1"
C2=$(cliente "33333333-3" "Transportes del Sur Ltda"   "sur@demo.cl");        echo "  Sur -> $C2"
C3=$(cliente "44444444-4" "Importadora Pacifico SA"     "pacifico@demo.cl");   echo "  Pacifico -> $C3"

echo "== Productos =="
for P in '{"codigo":"SRV01","nombre":"Servicio de flete","descripcion":"Flete nacional","unidadMedida":"UN","precio":50000,"afectaIva":true,"activo":true}' \
         '{"codigo":"SRV02","nombre":"Almacenaje","descripcion":"Bodega por dia","unidadMedida":"DIA","precio":30000,"afectaIva":true,"activo":true}' \
         '{"codigo":"PRD01","nombre":"Caja de carton","descripcion":"Caja 60x40","unidadMedida":"UN","precio":1500,"afectaIva":true,"activo":true}'; do
  curl -s -o /dev/null -w "  producto -> HTTP %{http_code}\n" -X POST "$B/productos" -H "$AUTH" -H "$JSON" -d "$P"
done

doc(){ # tipo clienteId  -> echoes id
  curl -s -X POST "$B/api/documentos" -H "$AUTH" -H "$JSON" -d "{\"codigoTipoDocumento\":$1,\"clienteId\":$2}" | jid
}
linea(){ curl -s -o /dev/null -X POST "$B/api/documentos/$1/detalles" -H "$AUTH" -H "$JSON" -d "{\"descripcion\":\"$2\",\"cantidad\":$3,\"precioUnitario\":$4}"; }
emitir(){ curl -s -o /dev/null -w "  emitir doc $1 -> HTTP %{http_code}\n" -X POST "$B/api/documentos/$1/emitir" -H "$AUTH"; }

echo "== Documentos =="
D1=$(doc 34 "$C1"); linea "$D1" "Flete aereo SCL-MIA" 1 850000; linea "$D1" "Handling" 1 50000; echo "  Doc 34 (BORRADOR) -> $D1"
D2=$(doc 33 "$C2"); linea "$D2" "Servicio de flete" 2 50000; echo "  Doc 33 -> $D2"; emitir "$D2"
D3=$(doc 34 "$C3"); linea "$D3" "Almacenaje" 3 30000;        echo "  Doc 34 -> $D3"; emitir "$D3"

echo "Listo. Datos de prueba creados en la empresa de root."
