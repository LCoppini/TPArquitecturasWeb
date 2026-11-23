# ADR-0015: Rechazar devolución parcial de una Compra múltiple

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Una Compra puede contener varias entradas y se procesa con un único pago. La pasarela actual solo permite reintegrar el pago completo; devolver una entrada sin soporte de importe parcial podría reintegrar accidentalmente el total o dejar una solicitud sin resolución posible.

## Decisión

Si se solicita devolver una entrada individual de una Compra con varias entradas, responder `409 Conflict` con un mensaje claro que indique que se puede devolver la Compra completa. No crear solicitudes pendientes de revisión ni modificar entradas o pago. Cada entrada conserva su precio individual para facilitar un change futuro con soporte real de reintegro parcial.

## Alternativas consideradas

- **Crear una solicitud de revisión manual:** descartada porque no existe una operación de pasarela que permita resolver el reintegro parcial y crearía una promesa sin salida definida.
- **Reintegrar el pago completo al solicitar una entrada:** descartada porque devolvería también el importe de las demás entradas.
- **Implementar reintegro parcial en este change:** fuera de alcance mientras la pasarela vigente no admita importes parciales.

## Consecuencias

Solo se permite la devolución completa de la Compra múltiple conforme a las reglas de devolución vigentes. La API puede explicar la alternativa disponible sin representar una solicitud no resoluble.

## Referencias

- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/proposal.md`
- `openspec/changes/fila-virtual/design.md`
