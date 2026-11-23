# ADR-0017: Cerrar tandas por resolución, agotamiento o vencimiento

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Una tanda debe habilitar hasta `maxPersonas` y consumir un cupo de entradas, sin imponer tiempos muertos cuando sus participantes ya resolvieron sus compras o el cupo se agotó. A la vez, una Compra pendiente puede vencer y devolver cupo, por lo que la tanda no debe cerrarse prematuramente mientras esa Compra siga `RESERVADA`.

## Decisión

Cerrar la tanda al ocurrir primero una de estas condiciones:

1. vence el turno de diez minutos desde el inicio;
2. todas las personas habilitadas compraron o salieron de la fila y no hay Compras `RESERVADA` de esa tanda;
3. el cupo restante llega a cero y no hay Compras `RESERVADA` que puedan devolver cupo.

Mientras haya una Compra `RESERVADA`, no cerrar por las condiciones 2 o 3. Al cerrar, abrir inmediatamente la siguiente tanda si la fila está `ACTIVA` y hay disponibilidad; sin disponibilidad y sin Compras en curso, pasar a `AGOTADA`. `PAUSADA` detiene aperturas, no los relojes; al reanudar se abre si corresponde. El cupo efectivo se recalcula como `min(cupoEntradas + remanenteAnterior, entradasDisponibles)`.

La verificación de cierre y apertura se coordina bajo el lock de fila del evento y actualizaciones condicionales. Una Compra pagada, vencida o rechazada dispara reevaluación; `@Scheduled` respalda el vencimiento. Eventos duplicados y ticks concurrentes no deben producir aperturas dobles.

## Alternativa descartada

Esperar un intervalo fijo de diez minutos entre tandas, incluso si todos terminan antes. Se descarta porque genera tiempos muertos cuando las compras se resuelven rápidamente y fija el ritmo de venta aunque haya personas esperando y entradas disponibles.

## Consecuencias

El ritmo de venta depende de que las personas resuelvan sus compras; el organizador debe dimensionar `maxPersonas` y `cupoEntradas` acorde. La implementación requiere transiciones atómicas para cierre/apertura y pruebas de concurrencia en PostgreSQL.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
- `openspec/changes/fila-virtual/tasks.md`
