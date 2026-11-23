# ADR-0013: Tope fijo de seis entradas por persona habilitada y Compra

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Cada persona habilitada puede adquirir entradas como una sola Compra grupal. El tamaño máximo acordado es seis entradas. El cupo de la tanda es una política separada de dosificación global: se configura independientemente de `maxPersonas`, tiene un mínimo de una entrada por persona habilitada y un máximo de seis por persona habilitada.

## Decisión

Mantener un máximo fijo de seis entradas por Compra. En la fila y en venta libre, cada cliente puede tener como máximo una Compra `RESERVADA` por evento a la vez; una Compra rechazada, vencida o pagada ya no bloquea la creación de otra mientras el flujo y, para la fila, el turno sigan vigentes. `cupoEntradas` no deriva de este límite: se configura independientemente entre `maxPersonas` y `maxPersonas * 6`, usando `maxPersonas * 2` por defecto. El cupo restante no reduce dinámicamente el máximo permitido por Compra; si la solicitud excede el cupo actual, se rechaza con `CUPO_TANDA_INSUFICIENTE` y se informa cuánto queda.

## Alternativa descartada

Calcular un límite individual variable dividiendo el cupo disponible por la cantidad de personas habilitadas, o reducir el máximo según el saldo de la tanda. Se descarta porque el máximo individual cambiaría según el momento y la concurrencia, mezclaría el límite por persona con el cupo de dosificación y haría impredecible la respuesta para quien compra.

## Consecuencias

La API valida siempre el límite de seis entradas y aplica por separado el descuento atómico del cupo de la tanda. El cliente puede recibir `409 CUPO_TANDA_INSUFICIENTE` aunque su selección cumpla el máximo individual; ese rechazo no limita a una sola Compra por turno.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
