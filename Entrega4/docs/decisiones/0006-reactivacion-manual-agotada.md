# ADR-0006: Reanudación manual de una fila AGOTADA

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

La disponibilidad puede llegar a cero y más tarde volver a aumentar por vencimientos o devoluciones. Reactivar automáticamente en cada fluctuación puede alternar repetidamente los estados y crear aperturas de tandas difíciles de anticipar.

## Decisión

Cuando no haya entradas disponibles al abrir una tanda y no existan compras en curso, la fila pasa a AGOTADA y conserva las secuencias de espera. No se reanuda automáticamente. El organizador propietario puede solicitar reanudar; la operación comprueba disponibilidad dentro de la transacción y solo habilita una tanda si existe inventario.

## Alternativa descartada

Reanudar automáticamente desde AGOTADA al detectar disponibilidad. Se descarta porque devoluciones intermitentes pueden provocar alternancia de estados y aperturas de fila impredecibles.

## Consecuencias

La disponibilidad liberada mientras la fila está AGOTADA no habilita personas por sí sola. Se requiere una acción del organizador y una validación atómica de inventario.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
