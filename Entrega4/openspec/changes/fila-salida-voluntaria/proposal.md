# Proposal: salida voluntaria de la fila y espera estimada

## Por qué
Un cliente EN_ESPERA solo puede dejar de estar en la fila si el organizador la cierra. El frontend necesita ofrecer "salir de la fila" y mostrar cuánto falta para el turno.

## Qué cambia
- msvc-entrada: `DELETE /api/filas-virtuales/eventos/{idEvento}/ingresos/mi` marca el ingreso EN_ESPERA como SALIO (idempotente). Si el cliente ya está HABILITADO responde 409.
- `IngresoFilaResponse` suma `esperaEstimadaSegundos` (cota superior: resto de la tanda abierta más 10 min por cada grupo de `maxPersonas` por delante). Es null si no hay tanda abierta o el cliente no está EN_ESPERA. El constructor previo se conserva.
- El gateway ya cubre `/api/filas-virtuales/**` para CLIENTE; no cambia.

## Fuera de alcance
- Salir estando HABILITADO (afecta el cupo de la tanda).
