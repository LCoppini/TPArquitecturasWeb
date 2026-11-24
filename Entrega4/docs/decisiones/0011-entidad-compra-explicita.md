# ADR-0011: Representar la compra agrupada con una entidad Compra

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

La API actual crea una entrada por operación y `Pago` referencia una entrada. La fila requiere reservar hasta seis entradas del mismo evento como unidad, cobrar un total único, hacerlas vencer juntas y poder resolver pago frente a vencimiento con una transición atómica. `RegistroPago` es un historial/enlace pago-entrada, no representa la operación antes de su pago ni su expiración.

## Decisión

Crear `Compra` como entidad propia dentro de `msvc-entrada`. Las entradas individuales se asocian a ella conservando precio, asiento, estado y QR propios. Un pago agrupado referencia Compra y `RegistroPago` conserva una fila por entrada para consulta e informes. Los pagos legacy continúan referenciando una entrada; los endpoints existentes se mantienen para eventos sin fila.

## Alternativa descartada

Agregar solo `idGrupoEntrada` a cada Entrada y deducir de sus filas el total, estado y vencimiento de la operación. Se descarta porque deja el grupo sin estado autoritativo, complica transiciones atómicas e idempotencia y obliga igualmente a cambiar la relación de Pago.

## Consecuencias

Se agregan tabla, relación y contratos en `msvc-entrada`, pero no se requiere que otros microservicios conozcan Compra: continúan consumiendo entradas individuales. `Pago` debe soportar referencias mutuamente excluyentes a Entrada legacy o Compra agrupada. Una devolución completa de Compra reembolsa el pago una sola vez; con la pasarela mock actual, devolver solo una entrada de una Compra múltiple responde 409 porque no hay reintegros por monto.

## Referencias

- `openspec/changes/fila-virtual/proposal.md`
- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
