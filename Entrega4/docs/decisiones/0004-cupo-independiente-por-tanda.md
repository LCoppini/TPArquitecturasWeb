# ADR-0004: Cupo de entradas independiente del tamaño de la tanda

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

La fila limita dos dimensiones distintas: cuántas personas pueden iniciar turno (`maxPersonas`) y cuántas entradas totales pueden reservarse en una tanda (`cupoEntradas`). Si cada tanda asignara automáticamente seis entradas por persona, el cupo dejaría de dosificar la venta.

## Decisión

Configurar `cupoEntradas` independientemente por evento al activar la fila. Si no se informa, usar `maxPersonas * 2`. Validar `maxPersonas <= cupoEntradas <= maxPersonas * 6` y que ambos parámetros no superen la disponibilidad real. No modificar los parámetros mientras la fila esté ACTIVA. A partir de la segunda tanda, sumar el remanente de la anterior antes de limitar el cupo efectivo por disponibilidad.

## Alternativa descartada

Derivar siempre `cupoEntradas = maxPersonas * 6`. Simplifica la configuración, pero elimina el cupo como mecanismo de dosificación y deja como único límite la disponibilidad física.

## Consecuencias

La apertura de tanda debe persistir y recalcular cupo efectivo; la compra debe descontarlo atómicamente. Se agrega una validación de configuración y un valor por defecto.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
