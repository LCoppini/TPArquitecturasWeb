# ADR-0009: Reingreso explícito después del vencimiento del turno

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Si una persona pierde su turno y vuelve automáticamente al final de la fila, ocuparía lugares del límite de personas habilitadas en tandas sucesivas aunque no esté activa.

## Decisión

Al vencer un turno sin reserva, la persona sale de la fila. Para volver a esperar debe solicitar un nuevo ingreso; se crea una secuencia nueva al final. El estado vencido se informa con un mensaje claro.

## Alternativa descartada

Devolver automáticamente al final de la espera a quien pierde el turno. Se descarta porque personas ausentes consumirían repetidamente lugares del límite `maxPersonas` de cada tanda.

## Consecuencias

El ingreso idempotente solo reusa una posición mientras siga vigente; una unión posterior a un vencimiento es una nueva admisión con nueva secuencia.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
