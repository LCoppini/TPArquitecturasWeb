# ADR-0014: Coordinar activación y reservas con advisory lock por evento

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Activar una fila debe ser mutuamente excluyente con crear reservas individuales. La fila aún puede no existir al momento de activarla, y reservar filas del evento o serializar todas las reservas reduciría concurrencia. La garantía debe funcionar entre varias instancias de `msvc-entrada`.

## Decisión

Usar advisory transaction locks de PostgreSQL con una clave estable derivada de `idEvento` y un namespace reservado para esta coordinación. La activación toma el lock exclusivo; la creación de reservas individuales y Compras lo toma compartido. El lock se conserva hasta commit o rollback. Tras obtenerlo, cada operación valida el estado de fila y el inventario; `UNIQUE(id_evento)` protege además contra la creación duplicada de filas.

## Alternativas consideradas

- **Aislamiento `SERIALIZABLE`:** descartado porque obliga a tratar abortos por serialización y reintentar operaciones completas, y no expresa tan directamente la exclusión activación-reserva que requiere compartir reservas concurrentes.
- **Bloqueo de fila del evento:** descartado porque serializa todas las reservas del mismo evento; además, la fila de la fila virtual no existe antes de activar y por eso no puede ser el lock común.
- **Lock en memoria de la aplicación:** descartado porque no coordina varias instancias.

## Consecuencias

Las reservas concurrentes de un evento pueden mantener concurrencia entre sí, mientras que la activación espera o prevalece según el orden transaccional del lock. El uso del lock debe ser uniforme en todos los caminos que creen reservas y de una variante `pg_advisory_xact_lock*`, no un lock de sesión.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
