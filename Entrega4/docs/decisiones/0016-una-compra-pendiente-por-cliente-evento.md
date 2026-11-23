# ADR-0016: Una Compra pendiente por cliente y evento

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Después del cierre manual de la fila, la venta grupal continúa sin turnos. Permitir varias Compras pendientes simultáneas del mismo cliente podría permitirle acaparar inventario antes de pagar o dejar vencer sus reservas.

## Decisión

Un cliente puede tener como máximo una Compra en estado `RESERVADA` por evento a la vez, incluso en venta libre. Si intenta crear otra Compra con una clave nueva mientras mantiene una pendiente, la API responde `409`. Cuando la Compra existente queda pagada, vencida o rechazada, puede crear otra Compra de hasta seis entradas; cada nueva Compra libre tiene su propia reserva de diez minutos desde su creación.

Garantizar la unicidad con un índice único parcial de PostgreSQL sobre `(id_usuario,id_evento)` donde `estado = 'RESERVADA'`. La restricción aplica entre instancias; un conflicto de unicidad se traduce a `409`. Los reintentos idempotentes de la misma solicitud devuelven el resultado existente y no se consideran una segunda Compra.

## Alternativa descartada

Permitir varias Compras pendientes simultáneas del mismo cliente/evento. Se descarta porque una persona podría acaparar inventario con varias reservas aún no pagadas.

## Consecuencias

La restricción de una Compra pendiente por cliente/evento es independiente del límite de seis entradas por Compra. Una Compra rechazada, vencida o pagada deja de bloquear otra; en el flujo de fila, el cliente puede iniciar una nueva mientras su turno siga vigente.

## Referencias

- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
- `openspec/changes/fila-virtual/tasks.md`
