# ADR-0018: El rechazo de pago no consume el turno

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Una Compra rechazada definitivamente libera sus asientos y devuelve el cupo de la tanda. El turno individual conserva su vencimiento original de diez minutos, y un rechazo de pago no debe impedir que el cliente complete la compra dentro de ese tiempo.

## Decisión

Solo puede existir una Compra `RESERVADA` por cliente y evento a la vez. Ante un rechazo definitivo, la Compra termina y sus reservas/cupo se compensan, pero el turno sigue vigente y permite crear otra Compra de hasta seis entradas. La nueva Compra hereda el mismo `venceEn`; no reinicia ni extiende el turno. Si la persona no vuelve a comprar, no se considera resuelta para cerrar la tanda por resolución completa hasta que compre o salga de la fila.

## Alternativa descartada

Hacer que un pago definitivamente rechazado consuma el turno e impida otra Compra hasta una nueva tanda o reingreso. Se descarta porque una falla de pago transitoria o corregible eliminaría injustificadamente el tiempo restante del turno, aunque la reserva y el cupo ya hayan sido compensados.

## Consecuencias

La idempotencia protege el reintento del mismo pago en curso; un rechazo terminal requiere una nueva Compra y su propia clave de idempotencia, dentro del vencimiento original. El cierre de tanda debe distinguir Compras pagadas de Compras rechazadas.

## Referencias

- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
- `openspec/changes/fila-virtual/tasks.md`
