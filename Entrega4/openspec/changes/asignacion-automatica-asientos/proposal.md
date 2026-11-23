# Proposal: asignación automática de asientos y estado de la fila

## Por qué
El frontend no muestra un mapa de asientos: el cliente elige sector y cantidad. Hoy `POST /api/compras` exige fila y asiento en los sectores numerados. Además el cliente no puede saber si un evento tiene fila virtual (condición para usar `/api/compras`).

## Qué cambia
- msvc-entrada: en `POST /api/compras`, si un item de un sector NUMERADO no trae fila ni asiento, el servicio asigna el primer asiento libre (por fila y luego por asiento), con el sector ya bloqueado. Los items con asiento explícito se validan como hasta ahora.
- msvc-entrada: `GET /api/filas-virtuales/eventos/{idEvento}` devuelve la fila del evento (404 si no tiene).
- La huella de idempotencia se calcula sobre el pedido original, por lo que reintentos con la misma clave devuelven la misma Compra.

## Fuera de alcance
- Mapa o listado de asientos ocupados.
- Compra individual (`POST /api/entradas`): conserva su contrato.

## Riesgos
- La asignación recorre el sector (capacidad acotada) dentro del lock del sector.
