# ADR-0008: Asignar cupo a solicitudes reales mediante lock transaccional

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Una persona habilitada podría no iniciar nunca una Compra. Reservar cupo anticipadamente para quien encabeza la cola le impediría usarlo a otras personas que sí están listas para comprar.

## Decisión

No se bloquea cupo para una persona solo por su posición. La asignación ocurre atómicamente cuando una persona con turno vigente envía una solicitud de Compra. Entre solicitudes elegibles, la asignación sigue el orden en que PostgreSQL concede el lock transaccional de la tanda; esto no garantiza FIFO por instante de llegada a la red.

## Alternativa descartada

Reservar cupo para la siguiente persona en espera antes de que inicie una Compra. Se descarta porque una persona habilitada inactiva frenaría a las demás. También se descarta una cola durable estrictamente FIFO por llegada de red, fuera del alcance de este change.

## Consecuencias

El cupo se descuenta mediante actualización condicional. La asignación sigue el orden del lock transaccional, no el instante de llegada de red; una cola durable estrictamente FIFO queda fuera de alcance de este change.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
